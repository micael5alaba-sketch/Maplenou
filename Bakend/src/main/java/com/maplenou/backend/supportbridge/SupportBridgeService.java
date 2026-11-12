package com.maplenou.backend.supportbridge;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.order.AddressSnapshot;
import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.returns.ReturnRequest;
import com.maplenou.backend.seller.SellerProfile;
import com.maplenou.backend.support.SupportTicket;
import com.maplenou.backend.supportbridge.SupportBridgeDtos.*;
import com.maplenou.backend.user.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lecture seule des données dont le Service client a besoin pour traiter un ticket (commande, paiement,
 * livraison, boutique, retour) et pour importer les anciens tickets. Aucune écriture, aucune donnée de
 * paiement sensible : montants, statuts et références uniquement.
 *
 * Requêtes JPQL locales (EntityManager) plutôt que de nouvelles méthodes dans les repositories des autres
 * modules : ce pont n'ajoute rien aux modules existants.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportBridgeService {

    static final String CURRENCY = "XOF";
    /** Nombre maximum d'éléments renvoyés par les listes « commandes d'un utilisateur » / « livraisons ». */
    static final int RECENT_LIMIT = 20;

    private final EntityManager em;

    // ---------------------------------------------------------------- commandes

    public Optional<OrderSummary> order(String orderNumber) {
        return findOrder(orderNumber).map(this::toOrder);
    }

    /** Commandes passées par l'utilisateur, ou contenant une sous-commande de sa boutique, les plus récentes d'abord. */
    public List<OrderSummary> ordersOf(UUID userId) {
        List<Order> orders = em.createQuery("""
                        SELECT o FROM Order o
                        WHERE o.buyer.id = :id
                           OR EXISTS (SELECT 1 FROM SubOrder so WHERE so.order = o AND so.shop.owner.id = :id)
                        ORDER BY o.createdAt DESC""", Order.class)
                .setParameter("id", userId)
                .setMaxResults(RECENT_LIMIT)
                .getResultList();
        return orders.stream().map(this::toOrder).toList();
    }

    private Optional<Order> findOrder(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return Optional.empty();
        }
        return em.createQuery("SELECT o FROM Order o WHERE o.orderNumber = :n", Order.class)
                .setParameter("n", orderNumber.trim())
                .getResultStream().findFirst();
    }

    private OrderSummary toOrder(Order o) {
        List<SubOrder> subs = o.getSubOrders();
        SubOrder single = subs.size() == 1 ? subs.get(0) : null;
        Shop shop = single == null ? null : single.getShop();
        String sellerName = shop != null ? shop.getName() : subs.size() + " boutiques";
        Delivery delivery = single == null ? null : deliveryOf(single).orElse(null);
        ReturnRequest ret = single == null ? null : returnOf(single).orElse(null);
        return new OrderSummary(o.getOrderNumber(), o.getBuyer().getId(), o.getBuyer().getFullName(),
                shop == null ? null : shop.getOwner().getId(), shop == null ? null : shop.getSlug(), sellerName,
                o.getCreatedAt(), o.getTotalAmount(), CURRENCY, o.getStatus().name(), o.getOrderNumber(),
                delivery == null ? null : delivery.getId().toString(), ret == null ? null : ret.getId().toString(),
                subs.stream().map(so -> new SubOrderSummary(so.getId().toString(), so.getShop().getName(),
                        so.getStatus().name(), so.getSubtotal())).toList());
    }

    // ---------------------------------------------------------------- paiement (référence = numéro de commande)

    public Optional<PaymentSummary> payment(String orderNumber) {
        return findOrder(orderNumber).map(o -> {
            BigDecimal commission = o.getSubOrders().stream().map(SubOrder::getCommissionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            SubOrder single = o.getSubOrders().size() == 1 ? o.getSubOrders().get(0) : null;
            String payoutStatus = single == null || single.getPayout() == null ? null
                    : single.getPayout().getStatus().name();
            BigDecimal payoutAmount = single == null ? null : single.getNetAmount();
            String provider = o.getFedapayTransactionId() != null ? "FEDAPAY" : null;
            return new PaymentSummary(o.getOrderNumber(), o.getOrderNumber(), o.getStatus().name(),
                    o.getTotalAmount(), CURRENCY, provider, commission, payoutStatus, payoutAmount,
                    o.getStatus().name().equals("CREATED") ? null : o.getUpdatedAt());
        });
    }

    // ---------------------------------------------------------------- livraisons

    public Optional<DeliverySummary> delivery(String ref) {
        return parseUuid(ref).flatMap(id -> em.createQuery("SELECT d FROM Delivery d WHERE d.id = :id", Delivery.class)
                .setParameter("id", id).getResultStream().findFirst()).map(this::toDelivery);
    }

    public List<DeliverySummary> deliveriesOf(UUID driverId) {
        return em.createQuery("SELECT d FROM Delivery d WHERE d.agent.id = :id ORDER BY d.createdAt DESC",
                        Delivery.class)
                .setParameter("id", driverId).setMaxResults(RECENT_LIMIT)
                .getResultList().stream().map(this::toDelivery).toList();
    }

    private Optional<Delivery> deliveryOf(SubOrder so) {
        return em.createQuery("SELECT d FROM Delivery d WHERE d.subOrder = :so", Delivery.class)
                .setParameter("so", so).getResultStream().findFirst();
    }

    private DeliverySummary toDelivery(Delivery d) {
        Order o = d.getSubOrder().getOrder();
        User agent = d.getAgent();
        AddressSnapshot a = o.getDeliveryAddress();
        String destination = a == null ? null : String.join(", ",
                java.util.stream.Stream.of(a.getDistrict(), a.getCity()).filter(s -> s != null && !s.isBlank()).toList());
        return new DeliverySummary(d.getId().toString(), o.getOrderNumber(), o.getBuyer().getFullName(),
                agent == null ? null : agent.getId(), agent == null ? null : agent.getFullName(), d.getStatus().name(),
                d.getAssignedAt(), d.getUpdatedAt() != null ? d.getUpdatedAt() : d.getCreatedAt(), destination, null);
    }

    // ---------------------------------------------------------------- vendeurs (référence = slug de la boutique)

    public Optional<SellerSummary> seller(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return em.createQuery("SELECT s FROM Shop s WHERE s.slug = :slug", Shop.class)
                .setParameter("slug", slug.trim()).getResultStream().findFirst().map(s -> {
                    User owner = s.getOwner();
                    SellerProfile profile = em.createQuery(
                                    "SELECT p FROM SellerProfile p WHERE p.user.id = :id", SellerProfile.class)
                            .setParameter("id", owner.getId()).getResultStream().findFirst().orElse(null);
                    String status = profile != null ? profile.getStatus().name() : s.getStatus().name();
                    return new SellerSummary(s.getSlug(), owner.getId(), s.getName(), owner.getFullName(), status,
                            null, CURRENCY);
                });
    }

    // ---------------------------------------------------------------- retours / remboursements

    public Optional<RefundSummary> refund(String ref) {
        return parseUuid(ref).flatMap(id -> em.createQuery("SELECT r FROM ReturnRequest r WHERE r.id = :id",
                        ReturnRequest.class).setParameter("id", id).getResultStream().findFirst())
                .map(r -> new RefundSummary(r.getId().toString(), r.getSubOrder().getOrder().getOrderNumber(),
                        r.getRefundStatus().name(), r.getSubOrder().getSubtotal(), CURRENCY, r.getCreatedAt()));
    }

    private Optional<ReturnRequest> returnOf(SubOrder so) {
        return em.createQuery("SELECT r FROM ReturnRequest r WHERE r.subOrder = :so", ReturnRequest.class)
                .setParameter("so", so).getResultStream().findFirst();
    }

    // ---------------------------------------------------------------- anciens tickets (import unique)

    public LegacyTicketPage legacyTickets(int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100));
        int safePage = Math.max(0, page);
        long total = em.createQuery("SELECT COUNT(t) FROM SupportTicket t", Long.class).getSingleResult();
        List<LegacyTicket> items = em.createQuery("SELECT t FROM SupportTicket t ORDER BY t.createdAt ASC, t.id ASC",
                        SupportTicket.class)
                .setFirstResult(safePage * safeSize).setMaxResults(safeSize)
                .getResultList().stream().map(this::toLegacy).toList();
        boolean last = (long) (safePage + 1) * safeSize >= total;
        return new LegacyTicketPage(items, safePage, safeSize, total, last);
    }

    private LegacyTicket toLegacy(SupportTicket t) {
        User u = t.getRequester();
        String role = u.getRole() != null ? u.getRole().name() : null;
        return new LegacyTicket(t.getId(), t.getTicketNumber(), u.getId(), u.getFullName(), u.getPhoneNumber(),
                u.getEmail(), role, t.getCategory(), t.getSubject(), t.getDescription(),
                t.getRelatedDelivery() == null ? null : t.getRelatedDelivery().getId().toString(), t.getPhotoUrl(),
                t.getStatus().name(), t.getResolutionNotes(), t.getResolvedAt(), t.getCreatedAt());
    }

    private static Optional<UUID> parseUuid(String ref) {
        try {
            return ref == null ? Optional.empty() : Optional.of(UUID.fromString(ref.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
