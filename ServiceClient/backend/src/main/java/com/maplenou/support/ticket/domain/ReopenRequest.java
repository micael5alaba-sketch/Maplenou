package com.maplenou.support.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/** Requester request to reopen a resolved ticket, decided by support (DEC-004, DEC-007). */
public final class ReopenRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    private final UUID id;
    private final UUID ticketId;
    private final UUID requestedBy;
    private final String reason;
    private Status status;
    private UUID decidedBy;
    private String decisionNote;
    private final Instant createdAt;
    private Instant decidedAt;

    public ReopenRequest(UUID id, UUID ticketId, UUID requestedBy, String reason, Status status, UUID decidedBy,
                         String decisionNote, Instant createdAt, Instant decidedAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.status = status;
        this.decidedBy = decidedBy;
        this.decisionNote = decisionNote;
        this.createdAt = createdAt;
        this.decidedAt = decidedAt;
    }

    public static ReopenRequest pending(UUID id, UUID ticketId, UUID requestedBy, String reason, Instant now) {
        return new ReopenRequest(id, ticketId, requestedBy, reason, Status.PENDING, null, null, now, null);
    }

    public void decide(boolean approved, UUID decidedBy, String note, Instant now) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Reopen request already decided");
        }
        this.status = approved ? Status.APPROVED : Status.REJECTED;
        this.decidedBy = decidedBy;
        this.decisionNote = note;
        this.decidedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID ticketId() {
        return ticketId;
    }

    public UUID requestedBy() {
        return requestedBy;
    }

    public String reason() {
        return reason;
    }

    public Status status() {
        return status;
    }

    public UUID decidedBy() {
        return decidedBy;
    }

    public String decisionNote() {
        return decisionNote;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant decidedAt() {
        return decidedAt;
    }
}
