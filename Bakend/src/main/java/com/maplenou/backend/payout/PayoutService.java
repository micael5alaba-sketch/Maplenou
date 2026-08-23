package com.maplenou.backend.payout;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.payout.dto.GeneratePayoutsRequest;
import com.maplenou.backend.payout.dto.PayoutResponse;
import com.maplenou.backend.payout.dto.UpdatePayoutStatusRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final SubOrderRepository subOrderRepository;
    private final AuditService auditService;

    // ----- Scheduler automatique -----

    /**
     * Génère les payouts hebdomadaires tous les lundis à 2h UTC.
     * Couvre la semaine précédente (lundi 00:00 → dimanche 23:59:59 UTC).
     */
    @Scheduled(cron = "0 0 2 * * MON", zone = "UTC")
    public void generateWeeklyPayoutsScheduled() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        // Semaine précédente
        ZonedDateTime lastMonday = now.with(DayOfWeek.MONDAY).minusWeeks(1).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime lastSunday  = lastMonday.plusDays(6)
                .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);

        log.info("[Payout] Génération automatique pour la période {} → {}",
                lastMonday, lastSunday);
        generatePayoutsForPeriod(lastMonday.toInstant(), lastSunday.toInstant());
    }

    // ----- Admin -----

    /**
     * Génération manuelle pour une période arbitraire (utile en test ou correction).
     */
    @Transactional
    public int generateManual(GeneratePayoutsRequest request, UUID actorId, String ip) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "periodEnd doit être postérieur à periodStart");
        }
        int count = generatePayoutsForPeriod(request.periodStart(), request.periodEnd());
        auditService.log(actorId, AuditAction.PAYOUT_GENERATED,
                "PAYOUT", null,
                "Génération manuelle : " + count + " payouts pour " +
                request.periodStart() + " → " + request.periodEnd(), ip);
        return count;
    }

    /** Admin : liste de tous les payouts (filtre optionnel par statut). */
    @Transactional(readOnly = true)
    public Page<PayoutResponse> listAll(PayoutStatus status, Pageable pageable) {
        return payoutRepository.findByOptionalStatus(status, pageable)
                .map(PayoutResponse::from);
    }

    /** Admin : détail d'un payout. */
    @Transactional(readOnly = true)
    public PayoutResponse getById(UUID id) {
        return PayoutResponse.from(getOrThrow(id));
    }

    /** Admin : marquer un payout comme payé. */
    @Transactional
    public PayoutResponse markAsPaid(UUID payoutId, UpdatePayoutStatusRequest request,
                                     UUID actorId, String ip) {
        Payout payout = getOrThrow(payoutId);

        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce payout est déjà marqué PAID");
        }

        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now());
        if (request.notes() != null) payout.setNotes(request.notes());
        payoutRepository.save(payout);

        auditService.log(actorId, AuditAction.PAYOUT_MARKED_PAID,
                "PAYOUT", payoutId,
                "Boutique : " + payout.getShop().getName() +
                " | Montant : " + payout.getAmount() + " FCFA" +
                (request.notes() != null ? " | " + request.notes() : ""), ip);

        return PayoutResponse.from(payout);
    }

    /** Admin : marquer un payout comme échoué (à retraiter). */
    @Transactional
    public PayoutResponse markAsFailed(UUID payoutId, UpdatePayoutStatusRequest request,
                                       UUID actorId, String ip) {
        Payout payout = getOrThrow(payoutId);

        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Impossible de marquer FAILED un payout déjà PAID");
        }

        payout.setStatus(PayoutStatus.FAILED);
        if (request.notes() != null) payout.setNotes(request.notes());
        payoutRepository.save(payout);

        auditService.log(actorId, AuditAction.PAYOUT_MARKED_FAILED,
                "PAYOUT", payoutId,
                "Boutique : " + payout.getShop().getName() +
                (request.notes() != null ? " | " + request.notes() : ""), ip);

        return PayoutResponse.from(payout);
    }

    /** Admin : passer un payout FAILED ou PENDING en PROCESSING. */
    @Transactional
    public PayoutResponse markAsProcessing(UUID payoutId, UUID actorId, String ip) {
        Payout payout = getOrThrow(payoutId);

        if (payout.getStatus() == PayoutStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Impossible de retraiter un payout déjà PAID");
        }

        payout.setStatus(PayoutStatus.PROCESSING);
        payoutRepository.save(payout);

        auditService.log(actorId, AuditAction.PAYOUT_PROCESSING,
                "PAYOUT", payoutId,
                "Boutique : " + payout.getShop().getName(), ip);

        return PayoutResponse.from(payout);
    }

    // ----- Vendeur -----

    /** Vendeur : ses payouts (par shopId, vérifié en amont dans le contrôleur). */
    @Transactional(readOnly = true)
    public Page<PayoutResponse> listForShop(UUID shopId, Pageable pageable) {
        return payoutRepository.findByShopIdOrderByCreatedAtDesc(shopId, pageable)
                .map(PayoutResponse::from);
    }

    // ----- Privé -----

    /**
     * Cœur de la logique de génération :
     * pour chaque boutique ayant des SubOrders DELIVERED non payés dans la période,
     * crée un Payout et lie les SubOrders.
     *
     * @return nombre de payouts créés
     */
    @Transactional
    public int generatePayoutsForPeriod(Instant from, Instant to) {
        List<UUID> shopIds = subOrderRepository.findDistinctShopIdsWithUnpaidDelivered(from, to);
        log.info("[Payout] {} boutique(s) éligibles pour la période {} → {}",
                shopIds.size(), from, to);

        int created = 0;
        for (UUID shopId : shopIds) {
            // Évite les doublons si le scheduler tourne deux fois
            if (payoutRepository.existsByShopIdAndPeriodStartAndPeriodEnd(shopId, from, to)) {
                log.warn("[Payout] Payout déjà existant pour shop {} sur cette période — ignoré", shopId);
                continue;
            }

            List<SubOrder> subOrders =
                    subOrderRepository.findDeliveredUnpaidByShopIdAndPeriod(shopId, from, to);
            if (subOrders.isEmpty()) continue;

            BigDecimal netTotal = subOrders.stream()
                    .map(SubOrder::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal commTotal = subOrders.stream()
                    .map(SubOrder::getCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Payout payout = Payout.builder()
                    .shop(subOrders.get(0).getShop())
                    .periodStart(from)
                    .periodEnd(to)
                    .amount(netTotal)
                    .commissionTotal(commTotal)
                    .status(PayoutStatus.PENDING)
                    .build();
            payout = payoutRepository.save(payout);

            // Lier les SubOrders au payout pour éviter de les réinclure la semaine suivante
            for (SubOrder so : subOrders) {
                so.setPayout(payout);
                subOrderRepository.save(so);
            }

            log.info("[Payout] Créé payout {} pour shop {} : {} FCFA net ({} sous-ordres)",
                    payout.getId(), shopId, netTotal, subOrders.size());
            created++;
        }
        return created;
    }

    private Payout getOrThrow(UUID id) {
        return payoutRepository.findByIdWithShop(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Payout introuvable"));
    }
}
