package com.maplenou.support.context.application;

import com.maplenou.support.context.application.port.EcosystemGateway;
import com.maplenou.support.context.domain.Ecosystem.DeliverySummary;
import com.maplenou.support.context.domain.Ecosystem.OrderSummary;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates and enriches the order/delivery references a requester attaches to a ticket (BR-019).
 * A requester may only link an order or delivery they take part in; anything else is answered with the same
 * "not found" error so other users' references are not disclosed (AC-SB-017).
 */
@Service
public class ReferenceService {

    /** Context references by type (ORDER, PAYMENT, DELIVERY, SELLER, REFUND). */
    public record ResolvedReferences(Map<String, String> refs) {
        public static final ResolvedReferences NONE = new ResolvedReferences(Map.of());
    }

    public record MyReferences(List<OrderSummary> orders, List<DeliverySummary> deliveries) {
    }

    private final EcosystemGateway gateway;

    public ReferenceService(EcosystemGateway gateway) {
        this.gateway = gateway;
    }

    public ResolvedReferences resolveForCreation(Actor user, String orderRef, String deliveryRef) {
        String order = blankToNull(orderRef);
        String delivery = blankToNull(deliveryRef);
        if (order == null && delivery == null) {
            return ResolvedReferences.NONE;
        }
        try {
            DeliverySummary d = null;
            if (delivery != null) {
                d = gateway.findDelivery(delivery).filter(x -> mayLinkDelivery(user, x))
                        .orElseThrow(ReferenceService::notAccessible);
                if (order == null) {
                    order = d.orderRef();
                }
            }
            OrderSummary o = null;
            if (order != null) {
                final DeliverySummary linkedDelivery = d;
                o = gateway.findOrder(order)
                        .filter(x -> mayLinkOrder(user, x)
                                // a driver may reference the order of their own delivery
                                || (user.role() == Role.DELIVERY_DRIVER && linkedDelivery != null
                                    && x.ref().equals(linkedDelivery.orderRef())))
                        .orElseThrow(ReferenceService::notAccessible);
                if (d != null && !o.ref().equals(d.orderRef())) {
                    throw SupportException.businessRule("REFERENCE_MISMATCH",
                            "La livraison indiquée ne correspond pas à cette commande.", "BR-019");
                }
            }
            Map<String, String> refs = new LinkedHashMap<>();
            if (o != null) {
                refs.put("ORDER", o.ref());
                refs.put("PAYMENT", o.paymentRef());
                refs.put("SELLER", o.sellerRef());
                refs.put("REFUND", o.refundRef());
            }
            refs.put("DELIVERY", d != null ? d.ref() : (o == null ? null : o.deliveryRef()));
            return new ResolvedReferences(refs);
        } catch (EcosystemGateway.EcosystemUnavailableException e) {
            throw SupportException.unavailable("ECOSYSTEM_UNAVAILABLE",
                    "Impossible de vérifier la commande pour le moment. Réessayez ou créez le ticket sans commande.");
        }
    }

    public MyReferences myReferences(Actor user) {
        try {
            return switch (user.role()) {
                case CUSTOMER, SELLER -> new MyReferences(gateway.ordersOf(user.id()), List.of());
                case DELIVERY_DRIVER -> new MyReferences(List.of(), gateway.deliveriesOf(user.id()));
                default -> new MyReferences(List.of(), List.of());
            };
        } catch (EcosystemGateway.EcosystemUnavailableException e) {
            return new MyReferences(List.of(), List.of());
        }
    }

    static boolean mayLinkOrder(Actor user, OrderSummary o) {
        return switch (user.role()) {
            case CUSTOMER -> user.id().equals(o.customerId());
            case SELLER -> user.id().equals(o.sellerUserId());
            case DELIVERY_DRIVER -> false; // drivers link through their delivery
            case ADMINISTRATOR -> true;
            default -> false;
        };
    }

    /** Drivers link their own deliveries; customers/sellers a delivery of one of their orders. */
    boolean mayLinkDelivery(Actor user, DeliverySummary d) {
        return switch (user.role()) {
            case DELIVERY_DRIVER -> user.id().equals(d.driverId());
            case CUSTOMER, SELLER -> d.orderRef() != null
                    && gateway.findOrder(d.orderRef()).filter(o -> mayLinkOrder(user, o)).isPresent();
            case ADMINISTRATOR -> true;
            default -> false;
        };
    }

    private static SupportException notAccessible() {
        return SupportException.businessRule("REFERENCE_NOT_ACCESSIBLE",
                "Commande ou livraison introuvable dans vos références.", "BR-019");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
