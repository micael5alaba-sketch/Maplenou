package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        String paymentReference,
        int subOrderCount,
        Instant createdAt
) {
    public static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPaymentReference(),
                order.getSubOrders().size(),
                order.getCreatedAt()
        );
    }
}
