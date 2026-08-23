package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubOrderResponse(
        UUID id,
        UUID orderId,
        UUID shopId,
        String shopName,
        SubOrderStatus status,
        BigDecimal subtotal,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        List<SubOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static SubOrderResponse from(SubOrder so) {
        return new SubOrderResponse(
                so.getId(),
                so.getOrder().getId(),
                so.getShop().getId(),
                so.getShop().getName(),
                so.getStatus(),
                so.getSubtotal(),
                so.getCommissionRate(),
                so.getCommissionAmount(),
                so.getNetAmount(),
                so.getItems().stream().map(SubOrderItemResponse::from).toList(),
                so.getCreatedAt(),
                so.getUpdatedAt()
        );
    }
}
