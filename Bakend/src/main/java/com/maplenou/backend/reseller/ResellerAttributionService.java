package com.maplenou.backend.reseller;

import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderItem;
import com.maplenou.backend.reseller.dto.ResellerDtos.CommissionResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Cycle de vie des commissions partenaire, branché sur les commandes :
 * <ul>
 *   <li>création de la commande : attribution GELÉE par ligne + une commission PENDING par unité
 *       (prix final après réduction, taux et part figés avec la version des paramètres) ;</li>
 *   <li>livraison validée : commissions créditées au portefeuille (idempotent) ;</li>
 *   <li>annulation / échec de paiement : commissions en attente annulées.</li>
 * </ul>
 * Aucune de ces opérations ne doit jamais empêcher une commande : les erreurs sont journalisées.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResellerAttributionService {

    private final ResellerSettingsService settingsService;
    private final ResellerLinkService linkService;
    private final ResellerClickRepository clickRepository;
    private final ResellerProfileRepository profileRepository;
    private final ResellerAttributionRepository attributionRepository;
    private final ResellerCommissionRepository commissionRepository;
    private final ResellerWalletService walletService;
    private final NotificationService notificationService;
    private final JdbcTemplate jdbc;

    /** Candidat d'attribution pour un produit : le clic valide le plus récent gagne. */
    private record Candidate(ResellerClick click, UUID partnerId) {
    }

    /**
     * Appelé à la création de la commande (après calcul de la remise). {@code tokens} sont les jetons
     * envoyés par l'appli ; les attributions liées au compte acheteur (autre appareil) sont aussi lues.
     */
    @Transactional
    public void attachToOrder(User buyer, Order order, List<String> tokens) {
        try {
            doAttach(buyer, order, tokens == null ? List.of() : tokens);
        } catch (RuntimeException e) {
            log.error("Attribution partenaire ignorée pour la commande {} : {}", order.getOrderNumber(), e.toString());
        }
    }

    private void doAttach(User buyer, Order order, List<String> tokens) {
        ResellerSettings settings = settingsService.current();
        if (!settings.isProgramActive()) return;
        Instant now = Instant.now();

        // 1. Clics candidats par produit (jetons de l'appareil + attributions du compte).
        Map<UUID, Candidate> best = new HashMap<>();
        for (String token : tokens) {
            linkService.clickIdFromToken(token).flatMap(clickRepository::findById)
                    .ifPresent(c -> consider(best, c, c.getPartnerId(), now));
        }
        jdbc.query("SELECT click_id FROM reseller_buyer_attributions WHERE user_id = ? AND expires_at > ?",
                rs -> {
                    clickRepository.findById(rs.getObject("click_id", UUID.class))
                            .ifPresent(c -> consider(best, c, c.getPartnerId(), now));
                }, buyer.getId(), Timestamp.from(now));
        if (best.isEmpty()) return;

        // 2. Part de remise à répartir sur les lignes (au prorata du sous-total produits).
        BigDecimal subtotals = order.getSubOrders().stream().map(SubOrder::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = order.getDiscountAmount() == null ? BigDecimal.ZERO : order.getDiscountAmount();
        BigDecimal keep = subtotals.signum() == 0 ? BigDecimal.ONE
                : BigDecimal.ONE.subtract(discount.divide(subtotals, 10, RoundingMode.HALF_UP)).max(BigDecimal.ZERO);

        for (SubOrder so : order.getSubOrders()) {
            for (SubOrderItem item : so.getItems()) {
                if (item.getProductVariant() == null) continue;
                UUID productId = item.getProductVariant().getProduct().getId();
                Candidate cand = best.get(productId);
                if (cand == null) continue;
                ResellerProfile partner = profileRepository.findById(cand.partnerId()).orElse(null);
                // Auto-parrainage interdit, partenaire doit être actif au moment de la commande.
                if (partner == null || !partner.isActivePartner()
                        || partner.getUser().getId().equals(buyer.getId())) continue;

                long unitPrice = item.getUnitPrice().multiply(keep).setScale(0, RoundingMode.HALF_UP).longValue();
                if (!ResellerCommissionCalculator.isEligible(unitPrice, settings)) continue;
                var calc = ResellerCommissionCalculator.compute(unitPrice, settings);

                ResellerAttribution attribution = attributionRepository.save(ResellerAttribution.builder()
                        .orderItemId(item.getId()).partnerId(partner.getId()).productId(productId)
                        .clickId(cand.click().getId()).buyerId(buyer.getId())
                        .clickedAt(cand.click().getClickedAt()).expiresAt(cand.click().getExpiresAt()).build());
                for (int unit = 1; unit <= item.getQuantity(); unit++) {
                    commissionRepository.save(ResellerCommission.builder()
                            .orderItemId(item.getId()).unitIndex((short) unit).attributionId(attribution.getId())
                            .partnerId(partner.getId()).productName(item.getSnapshotProductName())
                            .unitPrice(unitPrice).commissionRate(calc.rate())
                            .maplenouCommission(calc.maplenouCommission()).partnerShare(calc.partnerShare())
                            .maplenouShare(calc.maplenouShare()).settingsVersion(settings.getVersion()).build());
                }
            }
        }
    }

    private void consider(Map<UUID, Candidate> best, ResellerClick click, UUID partnerId, Instant now) {
        if (!click.isValid() || click.getExpiresAt().isBefore(now)) return;
        Candidate current = best.get(click.getProductId());
        if (current == null || click.getClickedAt().isAfter(current.click().getClickedAt())) {
            best.put(click.getProductId(), new Candidate(click, partnerId));
        }
    }

    /** Livraison validée : crédite les commissions en attente des lignes de cette sous-commande. */
    @Transactional
    public void onSubOrderDelivered(SubOrder subOrder) {
        try {
            List<UUID> itemIds = subOrder.getItems().stream().map(SubOrderItem::getId).toList();
            if (itemIds.isEmpty()) return;
            Map<UUID, Long> creditedPerPartner = new HashMap<>();
            for (ResellerCommission c : commissionRepository
                    .findByOrderItemIdInAndStatus(itemIds, ResellerCommissionStatus.PENDING)) {
                boolean written = walletService.append(c.getPartnerId(), WalletTxType.COMMISSION_CREDIT,
                        c.getPartnerShare(), c.getPartnerShare(), 0, 0, c.getId(), null,
                        "Commission " + c.getProductName(), null, "commission:" + c.getId());
                c.setStatus(ResellerCommissionStatus.CREDITED);
                c.setCreditedAt(Instant.now());
                commissionRepository.save(c);
                if (written) creditedPerPartner.merge(c.getPartnerId(), c.getPartnerShare(), Long::sum);
            }
            creditedPerPartner.forEach((partnerId, total) -> profileRepository.findByIdWithUser(partnerId)
                    .ifPresent(p -> notificationService.resellerEvent(p.getUser().getId(), "Commission créditée",
                            "+" + total + " FCFA ajoutés à votre portefeuille partenaire.", "RESELLER_COMMISSION",
                            partnerId)));
        } catch (RuntimeException e) {
            log.error("Crédit des commissions partenaire échoué pour la sous-commande {} : {}", subOrder.getId(), e.toString());
            throw e; // le crédit doit être rejoué avec la livraison plutôt que perdu en silence
        }
    }

    /** Commande annulée ou paiement échoué : les commissions non encore créditées sont annulées. */
    @Transactional
    public void onOrderCancelled(Order order) {
        List<UUID> itemIds = order.getSubOrders().stream()
                .flatMap(so -> so.getItems().stream()).map(SubOrderItem::getId).toList();
        cancelPending(itemIds);
    }

    /**
     * Retour approuvé : les commissions déjà créditées sont reprises par une écriture de régularisation
     * négative (le registre n'est jamais modifié), celles en attente sont annulées.
     */
    @Transactional
    public void onSubOrderReturned(SubOrder subOrder) {
        List<UUID> itemIds = subOrder.getItems().stream().map(SubOrderItem::getId).toList();
        if (itemIds.isEmpty()) return;
        for (ResellerCommission c : commissionRepository.findByOrderItemIdIn(itemIds)) {
            if (c.getStatus() == ResellerCommissionStatus.CREDITED) {
                walletService.append(c.getPartnerId(), WalletTxType.ADJUSTMENT, -c.getPartnerShare(),
                        -c.getPartnerShare(), 0, 0, c.getId(), null, "Retour produit : " + c.getProductName(), null,
                        "commission-return:" + c.getId());
                c.setStatus(ResellerCommissionStatus.CANCELLED);
                commissionRepository.save(c);
            } else if (c.getStatus() == ResellerCommissionStatus.PENDING) {
                c.setStatus(ResellerCommissionStatus.CANCELLED);
                commissionRepository.save(c);
            }
        }
    }

    @Transactional
    public void onSubOrderCancelled(SubOrder subOrder) {
        cancelPending(subOrder.getItems().stream().map(SubOrderItem::getId).toList());
    }

    private void cancelPending(List<UUID> itemIds) {
        if (itemIds.isEmpty()) return;
        for (ResellerCommission c : commissionRepository
                .findByOrderItemIdInAndStatus(itemIds, ResellerCommissionStatus.PENDING)) {
            c.setStatus(ResellerCommissionStatus.CANCELLED);
            commissionRepository.save(c);
        }
    }

    @Transactional(readOnly = true)
    public Page<CommissionResponse> myCommissions(UUID partnerId, Pageable pageable) {
        return commissionRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId, pageable)
                .map(ResellerAttributionService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CommissionResponse> allCommissions(Pageable pageable) {
        return commissionRepository.findAllByOrderByCreatedAtDesc(pageable).map(ResellerAttributionService::toResponse);
    }

    static CommissionResponse toResponse(ResellerCommission c) {
        return new CommissionResponse(c.getId(), c.getOrderItemId(), c.getPartnerId(), c.getProductName(),
                c.getUnitIndex(), c.getUnitPrice(), c.getCommissionRate(), c.getMaplenouCommission(),
                c.getPartnerShare(), c.getMaplenouShare(), c.getSettingsVersion(), c.getStatus(), c.getCreatedAt(),
                c.getCreditedAt());
    }
}
