package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.ticket.domain.ReopenRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_reopen_requests")
class ReopenRequestEntity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;
    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;
    @Column(nullable = false, updatable = false)
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReopenRequest.Status status;
    @Column(name = "decided_by")
    private UUID decidedBy;
    @Column(name = "decision_note")
    private String decisionNote;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "decided_at")
    private Instant decidedAt;

    protected ReopenRequestEntity() {
    }

    ReopenRequestEntity(UUID id) {
        super(id);
    }

    void copyFrom(ReopenRequest r) {
        this.ticketId = r.ticketId();
        this.requestedBy = r.requestedBy();
        this.reason = r.reason();
        this.status = r.status();
        this.decidedBy = r.decidedBy();
        this.decisionNote = r.decisionNote();
        this.createdAt = r.createdAt();
        this.decidedAt = r.decidedAt();
    }

    ReopenRequest toDomain() {
        return new ReopenRequest(getId(), ticketId, requestedBy, reason, status, decidedBy, decisionNote, createdAt,
                decidedAt);
    }
}
