package com.maplenou.support.ticket.domain.vocabulary;

/**
 * What a status means to the rules. Statuses are configurable (table {@code ticket_statuses}); the code only
 * relies on their kind. Exactly one <em>system</em> status exists for each kind except {@link #ON_HOLD}.
 */
public enum StatusKind {
    /** Just created, nobody assigned (BR-009). */
    NEW,
    /** Assigned, no support answer yet. */
    ASSIGNED,
    /** Support is working on it. Custom statuses may use this kind. */
    IN_PROGRESS,
    /** Support waits for the requester's answer; the requester's reply moves the ticket back to work. */
    WAITING_REQUESTER,
    /** Waiting for someone else (a seller, a carrier, a bank…). Custom statuses only. */
    ON_HOLD,
    /** Solution given; the requester may ask to reopen (BR-048). */
    RESOLVED,
    /** Final. */
    CLOSED;

    public boolean isOpen() {
        return this != RESOLVED && this != CLOSED;
    }

    /** Kinds an administrator may give to a status they add. */
    public boolean allowedForCustomStatus() {
        return this == IN_PROGRESS || this == ON_HOLD;
    }
}
