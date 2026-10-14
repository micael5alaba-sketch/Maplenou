package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.CourierEarning;
import com.maplenou.backend.delivery.CourierEarningStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourierEarningResponse(
        UUID id,
        UUID deliveryId,
        BigDecimal grossAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal amount,
        CourierEarningStatus status,
        Instant paidAt,
        Instant createdAt
) {
    public static CourierEarningResponse from(CourierEarning e) {
        return new CourierEarningResponse(
                e.getId(),
                e.getDelivery().getId(),
                e.getGrossAmount(),
                e.getCommissionRate(),
                e.getCommissionAmount(),
                e.getAmount(),
                e.getStatus(),
                e.getPaidAt(),
                e.getCreatedAt()
        );
    }
}
