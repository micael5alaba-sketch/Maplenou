package com.maplenou.support.notification.domain;

/** Notification triggers (roadmap Oct. week 4, FR-013). */
public enum NotificationType {
    TICKET_CREATED,
    NEW_TICKET_IN_QUEUE,
    TICKET_ASSIGNED,
    AGENT_REPLIED,
    REQUESTER_REPLIED,
    STATUS_CHANGED,
    INFORMATION_REQUESTED,
    TICKET_RESOLVED,
    TICKET_CLOSED,
    REOPEN_REQUESTED,
    REOPEN_DECIDED,
    TICKET_ESCALATED
}
