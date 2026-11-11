package com.maplenou.backend.admin;

import com.maplenou.backend.admin.dto.RoleDashboardResponse;
import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditLogRepository;
import com.maplenou.backend.audit.AuditLogResponse;
import com.maplenou.backend.courier.CourierDocumentStatus;
import com.maplenou.backend.delivery.CourierEarningStatus;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.security.Permission;
import com.maplenou.backend.security.RolePermissions;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.sellerfee.SellerFeeCategoryChangeStatus;
import com.maplenou.backend.support.SupportTicketStatus;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Tableaux de bord dédiés par rôle : chaque rôle voit les indicateurs de SON travail
 * (logistique, support, retraits, audit), jamais ceux d'un domaine qu'il ne peut pas lire.
 * Les sections suivent exactement les permissions de lecture (RolePermissions).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleDashboardService {

    private static final Duration ONLINE_WINDOW = Duration.ofMinutes(10);

    @PersistenceContext
    private EntityManager em;

    private final AuditLogRepository auditLogRepository;

    /** [viewAs] : aperçu du tableau de bord d'un autre rôle (Super Admin, vérifié par le contrôleur). */
    public RoleDashboardResponse get(User viewer, Role viewAs) {
        Set<Permission> allowed = viewAs != null ? RolePermissions.forRole(viewAs) : viewer.permissions();
        Instant now = Instant.now();
        return new RoleDashboardResponse(
                allowed.contains(Permission.DELIVERIES_READ) ? logistics(allowed, now) : null,
                allowed.contains(Permission.SUPPORT_READ) ? support(now) : null,
                allowed.contains(Permission.PAYOUTS_READ) ? finance(now, allowed) : null,
                allowed.contains(Permission.AUDIT_READ) ? audit(now) : null
        );
    }

    // ----- Logistique -----

    private RoleDashboardResponse.Logistics logistics(Set<Permission> allowed, Instant now) {
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekAgo = now.minus(Duration.ofDays(7));

        Instant oldestPending = single("SELECT MIN(d.createdAt) FROM Delivery d WHERE d.status = :s",
                "s", DeliveryStatus.PENDING);

        @SuppressWarnings("unchecked")
        List<Object[]> times = em.createQuery("SELECT d.assignedAt, d.deliveredAt FROM Delivery d "
                        + "WHERE d.status = :s AND d.deliveredAt >= :since AND d.assignedAt IS NOT NULL")
                .setParameter("s", DeliveryStatus.DELIVERED)
                .setParameter("since", weekAgo)
                .setMaxResults(2000)
                .getResultList();
        Double average = times.isEmpty() ? null : times.stream()
                .mapToLong(r -> Duration.between((Instant) r[0], (Instant) r[1]).toMinutes())
                .average().orElse(0);

        Long docs = allowed.contains(Permission.COURIERS_READ)
                ? count("SELECT COUNT(c) FROM CourierDocument c WHERE c.status = :s", "s", CourierDocumentStatus.PENDING)
                : null;
        BigDecimal earnings = allowed.contains(Permission.COURIER_EARNINGS_READ)
                ? single("SELECT COALESCE(SUM(e.amount), 0) FROM CourierEarning e WHERE e.status = :s AND e.withdrawal IS NULL",
                        "s", CourierEarningStatus.PENDING)
                : null;

        return new RoleDashboardResponse.Logistics(
                count("SELECT COUNT(d) FROM Delivery d WHERE d.status = :s", "s", DeliveryStatus.PENDING),
                count("SELECT COUNT(d) FROM Delivery d WHERE d.status = :s", "s", DeliveryStatus.ASSIGNED),
                count("SELECT COUNT(d) FROM Delivery d WHERE d.status = :s", "s", DeliveryStatus.IN_TRANSIT),
                count("SELECT COUNT(d) FROM Delivery d WHERE d.status = :s AND d.deliveredAt >= :since",
                        "s", DeliveryStatus.DELIVERED, "since", startOfDay),
                count("SELECT COUNT(d) FROM Delivery d WHERE d.status = :s", "s", DeliveryStatus.FAILED),
                oldestPending != null ? Duration.between(oldestPending, now).toMinutes() : null,
                average,
                count("SELECT COUNT(l) FROM CourierLocation l WHERE l.updatedAt >= :since AND l.agent.active = true",
                        "since", now.minus(ONLINE_WINDOW)),
                count("SELECT COUNT(u) FROM User u WHERE u.role = :r AND u.active = true", "r", Role.DELIVERY_AGENT),
                docs,
                earnings
        );
    }

    // ----- Support -----

    private RoleDashboardResponse.Support support(Instant now) {
        Instant oldestOpen = single("SELECT MIN(t.createdAt) FROM SupportTicket t WHERE t.status = :s",
                "s", SupportTicketStatus.OPEN);
        return new RoleDashboardResponse.Support(
                count("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :s", "s", SupportTicketStatus.OPEN),
                count("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :s", "s", SupportTicketStatus.IN_PROGRESS),
                count("SELECT COUNT(t) FROM SupportTicket t WHERE t.status = :s AND t.resolvedAt >= :since",
                        "s", SupportTicketStatus.RESOLVED, "since", now.minus(Duration.ofDays(7))),
                oldestOpen != null ? Duration.between(oldestOpen, now).toHours() : null,
                count("SELECT COUNT(sp) FROM SellerProfile sp WHERE sp.status = :s", "s", SellerStatus.PENDING),
                count("SELECT COUNT(c) FROM SellerFeeCategoryChangeRequest c WHERE c.status = :s",
                        "s", SellerFeeCategoryChangeStatus.PENDING),
                count("SELECT COUNT(r) FROM ReturnRequest r WHERE r.decision IS NULL"),
                unreadSupportMessages(),
                count("SELECT COUNT(r) FROM ResellerProfile r WHERE r.status = :s",
                        "s", com.maplenou.backend.reseller.ResellerStatus.PENDING)
        );
    }

    /** Messages adressés au support (envoyés par un non-membre de l'équipe) pas encore lus. */
    private long unreadSupportMessages() {
        return count("SELECT COUNT(m) FROM Message m JOIN m.conversation c JOIN m.sender sd "
                        + "WHERE c.type = :t AND m.readAt IS NULL "
                        + "AND (sd.role IS NULL OR sd.role NOT IN :staff)",
                "t", com.maplenou.backend.messaging.ConversationType.SUPPORT,
                "staff", java.util.List.of(Role.ADMIN, Role.DELIVERY_ADMIN, Role.SUPPORT_AGENT, Role.AUDITOR));
    }

    // ----- Retraits (vendeurs + livreurs) -----

    private RoleDashboardResponse.Finance finance(Instant now, Set<Permission> allowed) {
        Instant weekAgo = now.minus(Duration.ofDays(7));
        long[] toVerify = countAndSum(List.of(PayoutStatus.PENDING));
        long[] toCorrect = countAndSum(List.of(PayoutStatus.NEEDS_CORRECTION));
        long[] prepared = countAndSum(List.of(PayoutStatus.PREPARED));
        BigDecimal[] awaiting = new BigDecimal[1];
        long awaitingCount = countAndSumAmount(List.of(PayoutStatus.SUBMITTED), awaiting);
        BigDecimal[] toPay = new BigDecimal[1];
        long toPayCount = countAndSumAmount(List.of(PayoutStatus.APPROVED, PayoutStatus.PROCESSING), toPay);

        BigDecimal paid = single("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.status = :s AND p.paidAt >= :since",
                "s", PayoutStatus.PAID, "since", weekAgo);
        BigDecimal paidCouriers = single("SELECT COALESCE(SUM(w.amount), 0) FROM CourierWithdrawal w WHERE w.status = :s AND w.paidAt >= :since",
                "s", PayoutStatus.PAID, "since", weekAgo);

        return new RoleDashboardResponse.Finance(
                toVerify[0], toCorrect[0], prepared[0],
                awaitingCount, awaiting[0],
                toPayCount, toPay[0],
                paid.add(paidCouriers),
                actionable("Payout", "p", allowed),
                actionable("CourierWithdrawal", "w", allowed),
                actionable("ResellerWithdrawal", "w", allowed)
        );
    }

    /**
     * Retraits sur lesquels ce rôle peut agir : préparer/soumettre (reçus, préparés) pour qui a
     * PAYOUTS_PREPARE, approuver (soumis) pour qui a PAYOUTS_APPROVE. Lecture seule : aucun.
     */
    private long actionable(String entity, String alias, Set<Permission> allowed) {
        java.util.List<PayoutStatus> statuses = new java.util.ArrayList<>();
        if (allowed.contains(Permission.PAYOUTS_PREPARE)) {
            statuses.add(PayoutStatus.PENDING);
            statuses.add(PayoutStatus.PREPARED);
        }
        if (allowed.contains(Permission.PAYOUTS_APPROVE)) {
            statuses.add(PayoutStatus.SUBMITTED);
        }
        if (statuses.isEmpty()) return 0L;
        return (Long) em.createQuery("SELECT COUNT(" + alias + ") FROM " + entity + " " + alias
                        + " WHERE " + alias + ".status IN :st")
                .setParameter("st", statuses).getSingleResult();
    }

    /** Nombre de retraits (vendeurs + livreurs) dans ces statuts. */
    private long[] countAndSum(List<PayoutStatus> statuses) {
        return new long[]{countAndSumAmount(statuses, new BigDecimal[1])};
    }

    /** Compte les retraits (vendeurs + livreurs) et place la somme des montants dans [sumOut][0]. */
    private long countAndSumAmount(List<PayoutStatus> statuses, BigDecimal[] sumOut) {
        Object[] sellers = (Object[]) em.createQuery(
                        "SELECT COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payout p WHERE p.status IN :st")
                .setParameter("st", statuses).getSingleResult();
        Object[] couriers = (Object[]) em.createQuery(
                        "SELECT COUNT(w), COALESCE(SUM(w.amount), 0) FROM CourierWithdrawal w WHERE w.status IN :st")
                .setParameter("st", statuses).getSingleResult();
        Object[] partners = (Object[]) em.createQuery(
                        "SELECT COUNT(w), COALESCE(SUM(w.amount), 0L) FROM ResellerWithdrawal w WHERE w.status IN :st")
                .setParameter("st", statuses).getSingleResult();
        sumOut[0] = ((BigDecimal) sellers[1]).add((BigDecimal) couriers[1])
                .add(BigDecimal.valueOf(((Number) partners[1]).longValue()));
        return (Long) sellers[0] + (Long) couriers[0] + (Long) partners[0];
    }

    // ----- Audit -----

    private RoleDashboardResponse.Audit audit(Instant now) {
        List<AuditLogResponse> recent = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10))
                .map(AuditLogResponse::from).getContent();
        return new RoleDashboardResponse.Audit(
                count("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since", "since", now.minus(Duration.ofHours(24))),
                count("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :a AND a.createdAt >= :since",
                        "a", AuditAction.USER_LOGIN_FAILED, "since", now.minus(Duration.ofHours(24))),
                count("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :a AND a.createdAt >= :since",
                        "a", AuditAction.ACCESS_DENIED, "since", now.minus(Duration.ofDays(7))),
                recent
        );
    }

    // ----- Utilitaires JPQL -----

    private long count(String jpql, Object... params) {
        Long value = single(jpql, params);
        return value != null ? value : 0L;
    }

    @SuppressWarnings("unchecked")
    private <T> T single(String jpql, Object... params) {
        Query query = em.createQuery(jpql);
        for (int i = 0; i < params.length; i += 2) {
            query.setParameter((String) params[i], params[i + 1]);
        }
        return (T) query.getSingleResult();
    }
}
