package com.maplenou.support.context.application;

import com.maplenou.support.context.application.port.EcosystemActions;
import com.maplenou.support.context.application.port.EcosystemGateway;
import com.maplenou.support.context.application.port.LegacyTicketSource;
import com.maplenou.support.context.domain.Ecosystem.DeliverySummary;
import com.maplenou.support.context.domain.Ecosystem.OrderSummary;
import com.maplenou.support.context.domain.Ecosystem.PaymentSummary;
import com.maplenou.support.context.domain.Ecosystem.RefundSummary;
import com.maplenou.support.context.domain.Ecosystem.SellerSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Public API of the context module: Maplenou data next to a ticket, actions on Maplenou, former tickets. */
@Service
public class EcosystemService {

    private static final Logger log = LoggerFactory.getLogger(EcosystemService.class);

    /** One part of the context: not linked, linked and found/not found, or linked but Maplenou unavailable. */
    public record Section<T>(boolean linked, boolean available, T data) {
        static <T> Section<T> notLinked() {
            return new Section<>(false, true, null);
        }
    }

    private final EcosystemGateway gateway;
    private final EcosystemActions actions;
    private final LegacyTicketSource legacy;

    public EcosystemService(EcosystemGateway gateway, EcosystemActions actions, LegacyTicketSource legacy) {
        this.gateway = gateway;
        this.actions = actions;
        this.legacy = legacy;
    }

    public Section<OrderSummary> order(String ref) {
        return section(ref, () -> gateway.findOrder(ref));
    }

    public Section<PaymentSummary> payment(String ref) {
        return section(ref, () -> gateway.findPayment(ref));
    }

    public Section<DeliverySummary> delivery(String ref) {
        return section(ref, () -> gateway.findDelivery(ref));
    }

    public Section<SellerSummary> seller(String ref) {
        return section(ref, () -> gateway.findSeller(ref));
    }

    public Section<RefundSummary> refund(String ref) {
        return section(ref, () -> gateway.findRefund(ref));
    }

    private <T> Section<T> section(String ref, Supplier<Optional<T>> lookup) {
        if (ref == null) {
            return Section.notLinked();
        }
        try {
            return new Section<>(true, true, lookup.get().orElse(null));
        } catch (RuntimeException e) {
            log.warn("Ecosystem lookup failed for {}: {}", ref, e.getMessage());
            return new Section<>(true, false, null);
        }
    }

    public EcosystemActions.ActionResult requestReturn(String orderRef, String subOrderId, String reason,
                                                       String condition, EcosystemActions.ActionContext ctx) {
        return actions.requestReturn(orderRef, subOrderId, reason, condition, ctx);
    }

    public EcosystemActions.ActionResult rescheduleDelivery(String deliveryRef, String reason,
                                                            EcosystemActions.ActionContext ctx) {
        return actions.rescheduleDelivery(deliveryRef, reason, ctx);
    }

    public EcosystemActions.ActionResult messageDriver(UUID driverId, String message, EcosystemActions.ActionContext ctx) {
        return actions.messageDriver(driverId, message, ctx);
    }

    public LegacyTicketSource.LegacyTicketPage legacyTickets(int page, int size) {
        return legacy.page(page, size);
    }
}
