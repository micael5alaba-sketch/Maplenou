package com.maplenou.support.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An action support asked the Maplenou platform to perform from a ticket (refund request, delivery released for a
 * new courier, message to the courier) and its outcome — kept for the audit (DEC-CS11-07).
 */
public record ExternalAction(UUID id, UUID ticketId, Type type, String targetRef, boolean succeeded, String resultRef,
                             String error, UUID requestedBy, Instant createdAt) {

    public enum Type { REFUND_REQUEST, DELIVERY_RESCHEDULE, DRIVER_MESSAGE }
}
