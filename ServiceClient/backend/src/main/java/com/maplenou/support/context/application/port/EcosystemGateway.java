package com.maplenou.support.context.application.port;

import com.maplenou.support.context.domain.Ecosystem.DeliverySummary;
import com.maplenou.support.context.domain.Ecosystem.OrderSummary;
import com.maplenou.support.context.domain.Ecosystem.PaymentSummary;
import com.maplenou.support.context.domain.Ecosystem.RefundSummary;
import com.maplenou.support.context.domain.Ecosystem.SellerSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port to the other Maplenou modules, read side. Two adapters: demo data, and Bakend's internal API
 * ({@code /internal/v1/**}, docs/architecture/ecosystem-integration.md).
 */
public interface EcosystemGateway {

    Optional<OrderSummary> findOrder(String orderRef);

    Optional<PaymentSummary> findPayment(String paymentRef);

    Optional<DeliverySummary> findDelivery(String deliveryRef);

    Optional<SellerSummary> findSeller(String sellerRef);

    Optional<RefundSummary> findRefund(String refundRef);

    /** Orders a customer bought (or a seller sold), most recent first. */
    List<OrderSummary> ordersOf(UUID userId);

    /** Deliveries assigned to a driver, most recent first. */
    List<DeliverySummary> deliveriesOf(UUID driverId);

    /** Raised when the remote module cannot be reached; callers degrade gracefully. */
    class EcosystemUnavailableException extends RuntimeException {
        public EcosystemUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
