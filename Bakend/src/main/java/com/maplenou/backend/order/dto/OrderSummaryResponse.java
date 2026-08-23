package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        OrderStatus status,
        BigDecimal totalAmount,
        String paymentReference,
        int subOrderCount,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentReference(),
                order.getSubOrders().size(),
                order.getCreatedAt()
        );
    }
}
