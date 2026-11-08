package com.maplenou.support.context.application.port;

import java.util.UUID;

/**
 * Port to Maplenou for the actions support starts from a ticket (DEC-CS11-07). Maplenou applies its own rules
 * (30-day return window, delivery status…) and audits the action; a refusal comes back as a SupportException with
 * Maplenou's message ({@code MAPLENOU_REFUSED}).
 */
public interface EcosystemActions {

    /** Context sent with every action, for Maplenou's audit log. */
    record ActionContext(String ticketNumber, String agentName, UUID agentId) {
    }

    record ActionResult(String ref, String status) {
    }

    /** Opens a return/refund request on behalf of the buyer. {@code subOrderId} is needed for multi-shop orders. */
    ActionResult requestReturn(String orderRef, String subOrderId, String reason, String productCondition,
                               ActionContext context);

    /** Releases the delivery (failed or stuck) so that a courier can take it again. */
    ActionResult rescheduleDelivery(String deliveryRef, String reason, ActionContext context);

    /** Sends a message to the courier in the Maplenou app (support conversation). */
    ActionResult messageDriver(UUID driverId, String message, ActionContext context);
}
