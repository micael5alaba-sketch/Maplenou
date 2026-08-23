package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubOrderSummary(
        UUID id,
        UUID shopId,
        String shopName,
        SubOrderStatus status,
        BigDecimal subtotal,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        Instant createdAt
) {
    public static SubOrderSummary from(SubOrder so) {
        return new SubOrderSummary(
                so.getId(),
                so.getShop().getId(),
                so.getShop().getName(),
                so.getStatus(),
                so.getSubtotal(),
                so.getCommissionAmount(),
                so.getNetAmount(),
                so.getCreatedAt()
        );
    }
}
