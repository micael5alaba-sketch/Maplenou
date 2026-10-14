package com.maplenou.backend.payout.dto;

import com.maplenou.backend.order.dto.SubOrderSummary;
import com.maplenou.backend.payout.Payout;
import com.maplenou.backend.payout.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Détail d'un reversement vendeur, avec le détail des sous-commandes incluses (pour la
 * "facture téléchargeable", cahier des charges §3.2/§4.2) — contrairement à
 * {@link PayoutResponse} qui ne donne que les totaux.
 */
public record PayoutDetailResponse(
        UUID id,
        UUID shopId,
        String shopName,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal amount,
        BigDecimal commissionTotal,
        PayoutStatus status,
        Instant paidAt,
        String notes,
        Instant createdAt,
        List<SubOrderSummary> subOrders
) {
    public static PayoutDetailResponse from(Payout p, List<SubOrderSummary> subOrders) {
        return new PayoutDetailResponse(
                p.getId(),
                p.getShop().getId(),
                p.getShop().getName(),
                p.getPeriodStart(),
                p.getPeriodEnd(),
                p.getAmount(),
                p.getCommissionTotal(),
                p.getStatus(),
                p.getPaidAt(),
                p.getNotes(),
                p.getCreatedAt(),
                subOrders
        );
    }
}
