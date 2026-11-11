package com.maplenou.backend.admin.dto;

import com.maplenou.backend.audit.AuditLogResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tableau de bord dédié à chaque rôle du back-office. Chaque section n'est renvoyée que si le
 * rôle a la permission de lecture correspondante ; sinon elle vaut {@code null}.
 */
public record RoleDashboardResponse(
        Logistics logistics,
        Support support,
        Finance finance,
        Audit audit
) {

    /** Vue logistique (Administrateur des livraisons) : permission DELIVERIES_READ. */
    public record Logistics(
            long pendingDeliveries,
            long assignedDeliveries,
            long inTransitDeliveries,
            long deliveredToday,
            long failedDeliveries,
            /** Ancienneté (minutes) de la plus ancienne livraison en attente d'affectation, null si aucune. */
            Long oldestPendingMinutes,
            /** Durée moyenne affectation → livraison sur 7 jours (minutes), null si aucune livraison. */
            Double averageDeliveryMinutes7d,
            long onlineCouriers,
            long activeCouriers,
            /** null si le rôle ne peut pas lire les livreurs / leurs documents. */
            Long courierDocumentsToReview,
            /** Gains livreurs pas encore inclus dans un retrait ; null si le rôle ne peut pas les lire. */
            BigDecimal courierEarningsAvailable
    ) {}

    /** Vue support (Agent support et retraits) : permission SUPPORT_READ. */
    public record Support(
            long openTickets,
            long inProgressTickets,
            long resolvedThisWeek,
            /** Ancienneté (heures) du plus ancien ticket ouvert, null si aucun. */
            Long oldestOpenTicketHours,
            long pendingSellerApplications,
            long pendingCategoryChanges,
            long pendingReturns,
            /** Messages des clients/vendeurs/livreurs au support, pas encore lus par l'équipe. */
            long unreadSupportMessages,
            /** Demandes d'adhésion au programme partenaire revendeur en attente de décision. */
            long pendingResellerRequests
    ) {}

    /** Retraits vendeurs ET livreurs cumulés : permission PAYOUTS_READ. */
    public record Finance(
            long toVerify,
            long toCorrect,
            long prepared,
            long awaitingApproval,
            BigDecimal awaitingApprovalAmount,
            long approvedToPay,
            BigDecimal approvedToPayAmount,
            BigDecimal paidLast7Days,
            /** Retraits vendeurs sur lesquels CE rôle peut agir maintenant (pastille du menu). */
            long actionRequiredSellerPayouts,
            /** Retraits livreurs sur lesquels CE rôle peut agir maintenant (pastille du menu). */
            long actionRequiredCourierWithdrawals,
            /** Retraits partenaires revendeurs sur lesquels CE rôle peut agir maintenant (pastille du menu). */
            long actionRequiredPartnerWithdrawals
    ) {}

    /** Vue audit (Auditeur, Super Admin) : permission AUDIT_READ. */
    public record Audit(
            long actionsLast24h,
            long failedLoginsLast24h,
            long accessDeniedLast7d,
            List<AuditLogResponse> recent
    ) {}
}
