package com.maplenou.support.context.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only views of the Maplenou objects a ticket refers to (FR-011): order, payment, delivery, seller, refund.
 * Same JSON shape as Bakend's internal API ({@code supportbridge.SupportBridgeDtos}).
 */
public final class Ecosystem {

    private Ecosystem() {
    }

    /** One shop's part of an order (the refund request needs it when the order has several shops). */
    public record SubOrderSummary(String id, String shopName, String status, BigDecimal subtotal) {
    }

    public record OrderSummary(String ref, UUID customerId, String customerName, UUID sellerUserId, String sellerRef,
                               String sellerName, Instant orderedAt, BigDecimal amount, String currency, String status,
                               String paymentRef, String deliveryRef, String refundRef,
                               List<SubOrderSummary> subOrders) {
    }

    public record PaymentSummary(String ref, String orderRef, String status, BigDecimal amount, String currency,
                                 String provider, BigDecimal commissionAmount, String payoutStatus,
                                 BigDecimal payoutAmount, Instant paidAt) {
    }

    public record DeliverySummary(String ref, String orderRef, String customerName, UUID driverId, String driverName,
                                  String status, Instant pickedUpAt, Instant lastUpdateAt, String destination,
                                  String timeSlot) {
    }

    public record SellerSummary(String ref, UUID sellerUserId, String shopName, String ownerName, String accountStatus,
                                BigDecimal balance, String currency) {
    }

    public record RefundSummary(String ref, String orderRef, String status, BigDecimal amount, String currency,
                                Instant requestedAt) {
    }
}
