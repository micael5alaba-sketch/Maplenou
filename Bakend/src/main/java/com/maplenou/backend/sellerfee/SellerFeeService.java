package com.maplenou.backend.sellerfee;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.payout.Payout;
import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.seller.SellerProfile;
import com.maplenou.backend.seller.SellerProfileRepository;
import com.maplenou.backend.sellerfee.dto.RequestCategoryChangeRequest;
import com.maplenou.backend.sellerfee.dto.SellerFeeSummaryResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Frais mensuel d'activité vendeur — voir "Maplenou — Exigences des nouveaux frais vendeurs" :
 * chaque boutique a droit à ses 3 premières ventes éligibles (sous-ordre DELIVERED, jamais
 * annulé/retourné) gratuites à vie ; à partir de la 4ᵉ, chaque mois civil avec au moins une vente
 * éligible génère EXACTEMENT un frais (jamais deux pour le même mois, voir la contrainte unique
 * {@code (shop_id, billing_month)} — migration V48). Le frais est déduit du reversement vendeur
 * avant tout versement (voir {@code PayoutService#generatePayoutsForPeriod}), partiellement si le
 * solde est insuffisant, le reliquat restant dû pour le prochain reversement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerFeeService {

    /** Nombre de ventes éligibles gratuites à vie, avant application du frais mensuel. */
    private static final int FREE_ELIGIBLE_SALES = 3;

    private final SellerMonthlyFeeRepository monthlyFeeRepository;
    private final SellerFeeDeductionRepository deductionRepository;
    private final SellerFeeCategoryChangeRequestRepository categoryChangeRequestRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final SubOrderRepository subOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ----- Hooks métier -----

    /**
     * Appelé juste après qu'un {@link SubOrder} soit passé à DELIVERED (voir
     * {@code DeliveryService#updateStatus}). Vérifie si c'est la 4ᵉ vente éligible à vie (ou
     * au-delà) de la boutique, et si oui, crée le frais du mois courant s'il n'existe pas déjà.
     */
    @Transactional
    public void onSubOrderDelivered(SubOrder subOrder) {
        Shop shop = subOrder.getShop();
        long lifetimeEligible = subOrderRepository.countByShopIdAndStatus(shop.getId(), SubOrderStatus.DELIVERED);
        if (lifetimeEligible <= FREE_ELIGIBLE_SALES) {
            return; // encore dans les 3 ventes gratuites
        }

        SellerProfile profile = sellerProfileRepository.findByUserId(shop.getOwner().getId()).orElse(null);
        if (profile == null) {
            log.warn("[SellerFee] Boutique {} sans profil vendeur associé — frais ignoré", shop.getId());
            return;
        }

        SellerFeeCategory category = resolveActiveCategory(profile);
        LocalDate billingMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);

        if (monthlyFeeRepository.findByShopIdAndBillingMonth(shop.getId(), billingMonth).isPresent()) {
            return; // déjà créé ce mois-ci -- idempotent
        }

        SellerMonthlyFee fee = SellerMonthlyFee.builder()
                .shop(shop)
                .billingMonth(billingMonth)
                .category(category)
                .amount(category.monthlyFeeAmount())
                .build();

        try {
            monthlyFeeRepository.save(fee);
        } catch (DataIntegrityViolationException e) {
            // Concurrence : un autre appel a créé le frais entre le check et le save.
            log.info("[SellerFee] Frais déjà créé en parallèle pour shop {} / {}", shop.getId(), billingMonth);
            return;
        }

        notificationService.sellerFeeActivated(
                profile.getUser().getId(), fee.getAmount(), categoryLabel(category), monthLabel(billingMonth));
    }

    /**
     * Appelé juste après qu'un {@link SubOrder} soit passé à RETURNED (voir
     * {@code ReturnRequestService#decide}). Si plus aucune vente éligible ne reste pour le mois
     * de livraison de cette vente, annule le frais de ce mois s'il n'a encore rien été payé.
     * Un frais déjà (partiellement) payé n'est jamais remboursé rétroactivement.
     */
    @Transactional
    public void onSubOrderReturned(SubOrder subOrder) {
        Delivery delivery = deliveryRepository.findBySubOrderId(subOrder.getId()).orElse(null);
        if (delivery == null || delivery.getDeliveredAt() == null) return;

        Shop shop = subOrder.getShop();
        Instant deliveredAt = delivery.getDeliveredAt();
        LocalDate billingMonth = deliveredAt.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        Instant monthStart = billingMonth.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = billingMonth.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long stillEligibleThisMonth = subOrderRepository.countEligibleByShopIdAndDeliveredAtBetween(
                shop.getId(), monthStart, monthEnd);
        if (stillEligibleThisMonth > 0) return; // d'autres ventes éligibles restent ce mois -- frais toujours dû

        monthlyFeeRepository.findByShopIdAndBillingMonth(shop.getId(), billingMonth).ifPresent(fee -> {
            if (fee.getStatus() == SellerMonthlyFeeStatus.PENDING && fee.getAmountPaid().signum() == 0) {
                fee.setStatus(SellerMonthlyFeeStatus.CANCELLED);
                monthlyFeeRepository.save(fee);
                log.info("[SellerFee] Frais {} annulé (plus aucune vente éligible en {})", fee.getId(), billingMonth);
            }
        });
    }

    // ----- Déduction au reversement -----

    @Transactional(readOnly = true)
    public BigDecimal getUnpaidFeesTotal(UUID shopId) {
        return monthlyFeeRepository
                .findByShopIdAndStatusInOrderByBillingMonthAsc(
                        shopId, List.of(SellerMonthlyFeeStatus.PENDING, SellerMonthlyFeeStatus.PARTIALLY_PAID))
                .stream()
                .map(SellerMonthlyFee::remaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Déduit ce qui est possible sur les frais impayés (du plus ancien mois au plus récent),
     * jusqu'à {@code availableAmount}. Le reliquat non couvert reste dû pour le prochain
     * reversement. Doit être appelé une fois le {@link Payout} déjà sauvegardé (pour la
     * traçabilité de chaque déduction).
     *
     * @return le montant total réellement déduit (toujours ≤ availableAmount)
     */
    @Transactional
    public BigDecimal applyDeduction(UUID shopId, BigDecimal availableAmount, Payout payout) {
        if (availableAmount == null || availableAmount.signum() <= 0) return BigDecimal.ZERO;

        List<SellerMonthlyFee> unpaid = monthlyFeeRepository.findByShopIdAndStatusInOrderByBillingMonthAsc(
                shopId, List.of(SellerMonthlyFeeStatus.PENDING, SellerMonthlyFeeStatus.PARTIALLY_PAID));
        if (unpaid.isEmpty()) return BigDecimal.ZERO;

        BigDecimal remainingBudget = availableAmount;
        BigDecimal totalDeducted = BigDecimal.ZERO;

        for (SellerMonthlyFee fee : unpaid) {
            if (remainingBudget.signum() <= 0) break;

            BigDecimal due = fee.remaining();
            BigDecimal toDeduct = due.min(remainingBudget);
            if (toDeduct.signum() <= 0) continue;

            fee.setAmountPaid(fee.getAmountPaid().add(toDeduct));
            fee.setStatus(fee.getAmountPaid().compareTo(fee.getAmount()) >= 0
                    ? SellerMonthlyFeeStatus.PAID : SellerMonthlyFeeStatus.PARTIALLY_PAID);
            monthlyFeeRepository.save(fee);

            deductionRepository.save(SellerFeeDeduction.builder()
                    .monthlyFee(fee)
                    .payout(payout)
                    .amount(toDeduct)
                    .build());

            remainingBudget = remainingBudget.subtract(toDeduct);
            totalDeducted = totalDeducted.add(toDeduct);
        }

        if (totalDeducted.signum() > 0) {
            notificationService.sellerFeeDeducted(payout.getShop().getOwner().getId(), totalDeducted);
        }
        return totalDeducted;
    }

    // ----- Vendeur -----

    @Transactional(readOnly = true)
    public SellerFeeSummaryResponse getSummary(User currentUser, UUID shopId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun profil vendeur pour ce compte"));

        SellerFeeCategory category = resolveActiveCategory(profile);
        BigDecimal unpaidTotal = getUnpaidFeesTotal(shopId);
        BigDecimal available = subOrderRepository.sumAvailableBalanceByShopId(shopId);
        BigDecimal estimatedNet = available.subtract(unpaidTotal);
        if (estimatedNet.signum() < 0) estimatedNet = BigDecimal.ZERO;

        SellerFeeCategory pendingRequested = categoryChangeRequestRepository
                .findFirstBySellerProfileIdAndStatus(profile.getId(), SellerFeeCategoryChangeStatus.PENDING)
                .map(SellerFeeCategoryChangeRequest::getRequestedCategory)
                .orElse(null);

        return new SellerFeeSummaryResponse(
                category, category.monthlyFeeAmount(), unpaidTotal, available, estimatedNet, pendingRequested);
    }

    @Transactional(readOnly = true)
    public Page<SellerMonthlyFee> getFeeHistory(UUID shopId, Pageable pageable) {
        return monthlyFeeRepository.findByShopIdOrderByBillingMonthDesc(shopId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerFeeDeduction> getDeductionHistory(UUID shopId, Pageable pageable) {
        return deductionRepository.findByShopId(shopId, pageable);
    }

    @Transactional
    public SellerFeeCategoryChangeRequest requestCategoryChange(User currentUser, RequestCategoryChangeRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun profil vendeur pour ce compte"));

        resolveActiveCategory(profile); // applique un éventuel changement déjà dû avant de comparer

        if (profile.getFeeCategory() == request.requestedCategory()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cette catégorie est déjà active sur votre compte");
        }
        if (categoryChangeRequestRepository.existsBySellerProfileIdAndStatus(
                profile.getId(), SellerFeeCategoryChangeStatus.PENDING)) {
            throw new ApiException(HttpStatus.CONFLICT, "Une demande de changement est déjà en attente");
        }

        SellerFeeCategoryChangeRequest change = SellerFeeCategoryChangeRequest.builder()
                .sellerProfile(profile)
                .requestedCategory(request.requestedCategory())
                .status(SellerFeeCategoryChangeStatus.PENDING)
                .build();
        return categoryChangeRequestRepository.save(change);
    }

    // ----- Admin -----

    @Transactional(readOnly = true)
    public Page<SellerFeeCategoryChangeRequest> listCategoryChangeRequests(
            SellerFeeCategoryChangeStatus status, Pageable pageable) {
        return categoryChangeRequestRepository.findByStatus(status, pageable);
    }

    @Transactional
    public SellerFeeCategoryChangeRequest decideCategoryChange(
            UUID requestId, boolean approve, String adminNotes, User admin, String ip) {
        SellerFeeCategoryChangeRequest request = categoryChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande introuvable"));

        if (request.getStatus() != SellerFeeCategoryChangeStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée");
        }

        request.setStatus(approve ? SellerFeeCategoryChangeStatus.APPROVED : SellerFeeCategoryChangeStatus.REJECTED);
        request.setDecidedBy(admin);
        request.setDecidedAt(Instant.now());
        request.setAdminNotes(adminNotes);
        if (approve) {
            // Jamais rétroactif : effectif seulement au 1er jour du mois suivant.
            request.setEffectiveFrom(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).plusMonths(1));
        }
        categoryChangeRequestRepository.save(request);

        auditService.log(admin.getId(),
                approve ? AuditAction.SELLER_FEE_CATEGORY_CHANGE_APPROVED : AuditAction.SELLER_FEE_CATEGORY_CHANGE_REJECTED,
                "SELLER_FEE_CATEGORY_CHANGE", requestId,
                "Boutique : " + request.getSellerProfile().getShopName() +
                " | Catégorie demandée : " + request.getRequestedCategory() +
                (adminNotes != null ? " | " + adminNotes : ""), ip);

        return request;
    }

    // ----- Privé -----

    /** Applique un éventuel changement de catégorie déjà dû, puis renvoie la catégorie active. */
    private SellerFeeCategory resolveActiveCategory(SellerProfile profile) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<SellerFeeCategoryChangeRequest> due = categoryChangeRequestRepository
                .findBySellerProfileIdAndStatusAndAppliedFalseAndEffectiveFromLessThanEqual(
                        profile.getId(), SellerFeeCategoryChangeStatus.APPROVED, today);

        for (SellerFeeCategoryChangeRequest request : due) {
            profile.setFeeCategory(request.getRequestedCategory());
            request.setApplied(true);
            categoryChangeRequestRepository.save(request);
        }
        if (!due.isEmpty()) {
            sellerProfileRepository.save(profile);
        }
        return profile.getFeeCategory();
    }

    private String categoryLabel(SellerFeeCategory category) {
        return category == SellerFeeCategory.HIGH_PRICE ? "Prix élevés" : "Standard";
    }

    private String monthLabel(LocalDate billingMonth) {
        return billingMonth.getMonth().name() + " " + billingMonth.getYear();
    }
}
