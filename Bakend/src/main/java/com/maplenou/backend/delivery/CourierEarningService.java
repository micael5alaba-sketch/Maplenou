package com.maplenou.backend.delivery;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.CourierEarningResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Rémunération livreur : un {@link CourierEarning} est créé automatiquement
 * quand une {@link Delivery} passe à DELIVERED (voir {@link DeliveryService#updateStatus}).
 *
 * Barème décidé avec le porteur du projet (même logique que la commission
 * vendeur, {@code CommissionService}) : le frais de livraison réel du sous-ordre
 * ({@link com.maplenou.backend.order.SubOrder#getDeliveryFee()}, calculé et figé au moment
 * de la commande par {@link DeliveryPricingService} — zone + distance réelle) est partagé
 * 25 % plateforme / 75 % livreur. Avant l'ajout de la tarification dynamique, ce montant
 * était déduit après coup par correspondance approximative de ville/quartier puisque
 * l'Order ne mémorisait aucun frais de livraison réel ; ce n'est plus le cas.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourierEarningService {

    /** Part gardée par la plateforme sur le tarif de livraison. */
    private static final BigDecimal PLATFORM_RATE = new BigDecimal("0.2500");

    private final CourierEarningRepository courierEarningRepository;
    private final AuditService auditService;

    /** Montant gardé par la plateforme sur un tarif de livraison brut donné. */
    public BigDecimal platformAmountFor(BigDecimal grossFee) {
        return grossFee.multiply(PLATFORM_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /** Montant net versable au livreur sur un tarif de livraison brut donné. */
    public BigDecimal courierAmountFor(BigDecimal grossFee) {
        return grossFee.subtract(platformAmountFor(grossFee));
    }

    /**
     * Estimation du gain pour le livreur avant même qu'une livraison ne soit
     * terminée (affichée dès qu'elle lui est assignée) — même calcul que
     * {@link #createForDelivery}, sans rien persister.
     */
    @Transactional(readOnly = true)
    public BigDecimal estimateCourierAmount(Delivery delivery) {
        return courierAmountFor(delivery.getSubOrder().getDeliveryFee());
    }

    @Transactional
    public void createForDelivery(Delivery delivery) {
        BigDecimal gross = delivery.getSubOrder().getDeliveryFee();
        BigDecimal commission = platformAmountFor(gross);
        BigDecimal net = gross.subtract(commission);

        CourierEarning earning = CourierEarning.builder()
                .delivery(delivery)
                .agent(delivery.getAgent())
                .grossAmount(gross)
                .commissionRate(PLATFORM_RATE)
                .commissionAmount(commission)
                .amount(net)
                .status(CourierEarningStatus.PENDING)
                .build();
        courierEarningRepository.save(earning);
    }

    @Transactional(readOnly = true)
    public Page<CourierEarningResponse> listMine(User agent, Pageable pageable) {
        return courierEarningRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), pageable)
                .map(CourierEarningResponse::from);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(User agent) {
        return courierEarningRepository.sumAvailableBalanceByAgentId(agent.getId());
    }

    // ----- Admin -----

    /** Admin : tous les gains livreurs, filtrables par statut. */
    @Transactional(readOnly = true)
    public Page<CourierEarningResponse> listAllForAdmin(CourierEarningStatus status, Pageable pageable) {
        return courierEarningRepository.findByOptionalStatus(status, pageable).map(CourierEarningResponse::from);
    }

    /** Admin : marquer un gain comme versé (constaté manuellement, comme les payouts vendeur). */
    @Transactional
    public CourierEarningResponse markAsPaid(UUID id, UUID actorId, String ip) {
        CourierEarning earning = courierEarningRepository.findByIdWithAgent(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Gain introuvable"));

        if (earning.getStatus() == CourierEarningStatus.PAID) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce gain est déjà marqué PAID");
        }

        earning.setStatus(CourierEarningStatus.PAID);
        earning.setPaidAt(Instant.now());
        courierEarningRepository.save(earning);

        auditService.log(actorId, AuditAction.COURIER_EARNING_MARKED_PAID,
                "COURIER_EARNING", id,
                "Livreur : " + earning.getAgent().getFullName() +
                " | Montant : " + earning.getAmount() + " FCFA", ip);

        return CourierEarningResponse.from(earning);
    }
}
