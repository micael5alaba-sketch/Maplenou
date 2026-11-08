package com.maplenou.support.ticket.domain.event;

/** What happened to a ticket (drives notifications, FR-013). */
public enum TicketEventType {
    TICKET_CREATED,
    TICKET_ASSIGNED,
    AGENT_REPLIED,
    REQUESTER_REPLIED,
    STATUS_CHANGED,
    REOPEN_REQUESTED,
    REOPEN_DECIDED,
    TICKET_ESCALATED
}
