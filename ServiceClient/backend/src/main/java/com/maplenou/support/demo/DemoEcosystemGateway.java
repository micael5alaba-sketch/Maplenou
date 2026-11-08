package com.maplenou.support.demo;


import com.maplenou.support.context.application.port.EcosystemGateway;
import com.maplenou.support.context.domain.Ecosystem.DeliverySummary;
import com.maplenou.support.context.domain.Ecosystem.OrderSummary;
import com.maplenou.support.context.domain.Ecosystem.PaymentSummary;
import com.maplenou.support.context.domain.Ecosystem.RefundSummary;
import com.maplenou.support.context.domain.Ecosystem.SellerSummary;
import com.maplenou.support.context.domain.Ecosystem.SubOrderSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * In-memory sample of the Maplenou ecosystem, consistent with the demo users. Lets the full
 * support workflow run before the order/payment/delivery services expose their APIs.
 */
public class DemoEcosystemGateway implements EcosystemGateway {


    private static final String XOF = "XOF";

    private final Map<String, OrderSummary> orders;
    private final Map<String, PaymentSummary> payments;
    private final Map<String, DeliverySummary> deliveries;
    private final Map<String, SellerSummary> sellers;
    private final Map<String, RefundSummary> refunds;

    public DemoEcosystemGateway() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        String jean = "Jean K.";
        String marie = "Boutique Marie K.";
        sellers = index(List.of(
                new SellerSummary("VND-0042", DemoUsers.SELLER_MARIE, marie, "Marie K.", "ACTIVE",
                        new BigDecimal("86500"), XOF)), SellerSummary::ref);
        orders = index(List.of(
                new OrderSummary("CMD-2026-0456", DemoUsers.CUSTOMER_JEAN, jean, DemoUsers.SELLER_MARIE, "VND-0042",
                        marie, now.minus(3, ChronoUnit.DAYS), new BigDecimal("48500"), XOF, "SHIPPED",
                        "PAY-2026-0456", "LIV-2026-0912", null, subOrders("CMD-2026-0456", "SHIPPED", "48500")),
                new OrderSummary("CMD-2026-0789", DemoUsers.CUSTOMER_JEAN, jean, DemoUsers.SELLER_MARIE, "VND-0042",
                        marie, now.minus(9, ChronoUnit.DAYS), new BigDecimal("62000"), XOF, "DELIVERED",
                        "PAY-2026-0789", "LIV-2026-0850", null, subOrders("CMD-2026-0789", "DELIVERED", "62000")),
                new OrderSummary("CMD-2026-0501", DemoUsers.CUSTOMER_JEAN, jean, DemoUsers.SELLER_MARIE, "VND-0042",
                        marie, now.minus(15, ChronoUnit.DAYS), new BigDecimal("15000"), XOF, "DELIVERED",
                        "PAY-2026-0501", "LIV-2026-0811", "RMB-2026-0031", subOrders("CMD-2026-0501", "DELIVERED", "15000"))), OrderSummary::ref);
        payments = index(List.of(
                new PaymentSummary("PAY-2026-0456", "CMD-2026-0456", "PAID", new BigDecimal("48500"), XOF, "FedaPay",
                        new BigDecimal("4850"), "PENDING", new BigDecimal("43650"), now.minus(3, ChronoUnit.DAYS)),
                new PaymentSummary("PAY-2026-0789", "CMD-2026-0789", "PAID", new BigDecimal("62000"), XOF, "FedaPay",
                        new BigDecimal("12400"), "NOT_PAID_OUT", new BigDecimal("49600"), now.minus(9, ChronoUnit.DAYS)),
                new PaymentSummary("PAY-2026-0501", "CMD-2026-0501", "PARTIALLY_REFUNDED", new BigDecimal("15000"), XOF,
                        "Stripe", new BigDecimal("1500"), "PAID_OUT", new BigDecimal("13500"),
                        now.minus(15, ChronoUnit.DAYS))), PaymentSummary::ref);
        deliveries = index(List.of(
                new DeliverySummary("LIV-2026-0912", "CMD-2026-0456", jean, DemoUsers.DRIVER_KOSSI, "Kossi D.",
                        "DISPUTED", now.minus(1, ChronoUnit.DAYS), now.minus(2, ChronoUnit.HOURS), "Lomé, Bè-Kpota",
                        "09:00–12:00"),
                new DeliverySummary("LIV-2026-0850", "CMD-2026-0789", jean, DemoUsers.DRIVER_KOSSI, "Kossi D.",
                        "DELIVERED", now.minus(7, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS), "Lomé, Tokoin",
                        "14:00–17:00"),
                new DeliverySummary("LIV-2026-0811", "CMD-2026-0501", jean, DemoUsers.DRIVER_KOSSI, "Kossi D.",
                        "DELIVERED", now.minus(13, ChronoUnit.DAYS), now.minus(13, ChronoUnit.DAYS), "Lomé, Adidogomé",
                        "09:00–12:00")), DeliverySummary::ref);
        refunds = index(List.of(
                new RefundSummary("RMB-2026-0031", "CMD-2026-0501", "REQUESTED", new BigDecimal("5000"), XOF,
                        now.minus(2, ChronoUnit.DAYS))), RefundSummary::ref);
    }

    private static List<SubOrderSummary> subOrders(String order, String status, String amount) {
        return List.of(new SubOrderSummary(order + "-1", "Boutique Marie K.", status, new BigDecimal(amount)));
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> key) {
        return items.stream().collect(Collectors.toUnmodifiableMap(key, Function.identity()));
    }

    @Override
    public Optional<OrderSummary> findOrder(String ref) {
        return Optional.ofNullable(ref).map(orders::get);
    }

    @Override
    public Optional<PaymentSummary> findPayment(String ref) {
        return Optional.ofNullable(ref).map(payments::get);
    }

    @Override
    public Optional<DeliverySummary> findDelivery(String ref) {
        return Optional.ofNullable(ref).map(deliveries::get);
    }

    @Override
    public Optional<SellerSummary> findSeller(String ref) {
        return Optional.ofNullable(ref).map(sellers::get);
    }

    @Override
    public Optional<RefundSummary> findRefund(String ref) {
        return Optional.ofNullable(ref).map(refunds::get);
    }

    @Override
    public List<OrderSummary> ordersOf(UUID userId) {
        return orders.values().stream()
                .filter(o -> userId.equals(o.customerId()) || userId.equals(o.sellerUserId()))
                .sorted(Comparator.comparing(OrderSummary::orderedAt).reversed())
                .toList();
    }

    @Override
    public List<DeliverySummary> deliveriesOf(UUID driverId) {
        return deliveries.values().stream()
                .filter(d -> driverId.equals(d.driverId()))
                .sorted(Comparator.comparing(DeliverySummary::lastUpdateAt).reversed())
                .toList();
    }
}
