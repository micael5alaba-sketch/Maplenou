package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String paymentReference,
        DeliveryAddressDto deliveryAddress,
        List<SubOrderSummary> subOrders,
        Instant createdAt,
        Instant updatedAt,
        /** URL de paiement FedaPay à ouvrir côté client — uniquement renseignée juste après
         * POST /api/orders (voir OrderService.placeOrder), null partout ailleurs (on ne
         * regénère pas un lien de paiement à chaque relecture de la commande). */
        String paymentUrl
) {
    public static OrderResponse from(Order order) {
        return from(order, null);
    }

    public static OrderResponse from(Order order, String paymentUrl) {
        var addr = order.getDeliveryAddress();
        var addrDto = addr == null ? null : new DeliveryAddressDto(
                addr.getLabel(),
                addr.getCity(),
                addr.getDistrict(),
                addr.getDetails(),
                addr.getLatitude(),
                addr.getLongitude()
        );
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDiscountAmount(),
                order.getPaymentReference(),
                addrDto,
                order.getSubOrders().stream().map(SubOrderSummary::from).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                paymentUrl
        );
    }
}
