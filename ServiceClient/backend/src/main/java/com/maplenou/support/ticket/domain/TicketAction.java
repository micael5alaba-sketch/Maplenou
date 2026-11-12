package com.maplenou.support.ticket.domain;

/**
 * Protected operations on a ticket. Each one is authorized independently (BR-071) from the permission matrix
 * (table {@code permission_rules}) and the ticket's state. The list of allowed actions is returned to clients so
 * the UI shows only what the caller may do (AC-UX-011, AC-SB-022); the API still enforces every check.
 */
public enum TicketAction {
    SEND_MESSAGE(Audience.BOTH),
    ADD_INTERNAL_NOTE(Audience.STAFF),
    VIEW_INTERNAL(Audience.STAFF),
    VIEW_CONTEXT(Audience.STAFF),
    VIEW_SLA(Audience.STAFF),
    ASSIGN_SELF(Audience.STAFF),
    REASSIGN(Audience.STAFF),
    CHANGE_STATUS(Audience.STAFF),
    RESOLVE(Audience.STAFF),
    CLOSE(Audience.STAFF),
    CHANGE_PRIORITY(Audience.STAFF),
    /** Give a priority requesters may not choose themselves (BR-036). */
    SET_CRITICAL(Audience.STAFF),
    CHANGE_CATEGORY(Audience.STAFF),
    ESCALATE(Audience.STAFF),
    REQUEST_REOPEN(Audience.REQUESTER),
    DECIDE_REOPEN(Audience.STAFF),
    /** Rate the answer once the ticket is resolved (customer satisfaction, DEC-CS11-05). */
    RATE(Audience.REQUESTER),
    /** Category/priority suggestion and reply draft from the assistant (DEC-CS11-08). */
    USE_AI_ASSIST(Audience.STAFF),
    /** Ask Maplenou (Bakend) to open a return/refund request for the order (DEC-CS11-07). */
    REQUEST_REFUND(Audience.STAFF),
    /** Release the delivery so a courier can take it again. */
    RESCHEDULE_DELIVERY(Audience.STAFF),
    /** Send a message to the courier of the delivery in the Maplenou app. */
    CONTACT_DRIVER(Audience.STAFF);

    /** Who may ever receive the action: a safety net when administrators edit the permission matrix. */
    public enum Audience { REQUESTER, STAFF, BOTH }

    private final Audience audience;

    TicketAction(Audience audience) {
        this.audience = audience;
    }

    public Audience audience() {
        return audience;
    }
}
