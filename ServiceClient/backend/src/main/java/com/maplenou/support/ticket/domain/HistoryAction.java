package com.maplenou.support.ticket.domain;

/** Audited ticket events (FR-009). */
public enum HistoryAction {
    CREATED,
    STATUS_CHANGED,
    PRIORITY_CHANGED,
    CATEGORY_CHANGED,
    ASSIGNED,
    REASSIGNED,
    MESSAGE_ADDED,
    INTERNAL_NOTE_ADDED,
    ATTACHMENT_ADDED,
    ESCALATED,
    REOPEN_REQUESTED,
    REOPEN_APPROVED,
    REOPEN_REJECTED,
    SLA_RECALCULATED,
    SLA_PAUSED,
    SLA_RESUMED,
    AUTO_CLOSED,
    RATED,
    AI_SUGGESTION_APPLIED,
    EXTERNAL_ACTION,
    /** The former Maplenou support module changed the status of an imported ticket (DEC-CS12-02). */
    LEGACY_UPDATED
}
