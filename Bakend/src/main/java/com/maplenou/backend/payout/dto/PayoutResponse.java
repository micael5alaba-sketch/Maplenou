package com.maplenou.backend.payout.dto;

import com.maplenou.backend.payout.Payout;
import com.maplenou.backend.payout.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayoutResponse(
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
        Instant createdAt
) {
    public static PayoutResponse from(Payout p) {
        return new PayoutResponse(
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
                p.getCreatedAt()
        );
    }
}
