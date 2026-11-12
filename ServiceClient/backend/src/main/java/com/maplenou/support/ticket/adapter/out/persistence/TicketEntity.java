package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.domain.ContextRefs;
import com.maplenou.support.ticket.domain.Ticket;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistence model of the ticket aggregate (tables support_tickets + ticket_context_refs). Adapter only. */
@Entity
@Table(name = "support_tickets")
class TicketEntity extends BaseEntity {

    @Column(name = "ticket_number", nullable = false, unique = true, updatable = false)
    private String ticketNumber;
    @Column(nullable = false, updatable = false)
    private String subject;
    @Column(nullable = false, updatable = false)
    private String description;
    @Column(name = "category_code", nullable = false)
    private String categoryCode;
    @Column(nullable = false)
    private String priority;
    @Column(name = "priority_rank", nullable = false)
    private int priorityRank;
    @Column(nullable = false)
    private String status;
    @Column(name = "requester_id", nullable = false, updatable = false)
    private UUID requesterId;
    @Enumerated(EnumType.STRING)
    @Column(name = "requester_role", nullable = false, updatable = false)
    private Role requesterRole;
    @Column(name = "requester_reported_urgent", nullable = false, updatable = false)
    private boolean requesterReportedUrgent;
    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;
    @Column(nullable = false)
    private boolean escalated;
    @Column(name = "escalated_at")
    private Instant escalatedAt;
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ticket_context_refs", joinColumns = @JoinColumn(name = "ticket_id"))
    @MapKeyColumn(name = "ref_type")
    @Column(name = "ref")
    @BatchSize(size = 100)
    private Map<String, String> contextRefs = new HashMap<>();
    @Column(name = "first_response_due_at", nullable = false)
    private Instant firstResponseDueAt;
    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;
    @Column(name = "resolution_due_at", nullable = false)
    private Instant resolutionDueAt;
    @Column(name = "resolution_period_started_at", nullable = false)
    private Instant resolutionPeriodStartedAt;
    @Column(name = "sla_paused_since")
    private Instant slaPausedSince;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @Column(name = "closed_at")
    private Instant closedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "legacy_id")
    private UUID legacyId;
    @Column(name = "legacy_number")
    private String legacyNumber;
    @Version
    private long version;

    protected TicketEntity() {
    }

    TicketEntity(UUID id) {
        super(id);
    }

    void copyFrom(Ticket t) {
        Ticket.Snapshot s = t.snapshot();
        this.ticketNumber = s.ticketNumber();
        this.subject = s.subject();
        this.description = s.description();
        this.categoryCode = s.categoryCode();
        this.priority = s.priorityCode();
        this.priorityRank = s.priorityRank();
        this.status = s.statusCode();
        this.requesterId = s.requesterId();
        this.requesterRole = s.requesterRole();
        this.requesterReportedUrgent = s.requesterReportedUrgent();
        this.assignedAgentId = s.assignedAgentId();
        this.escalated = s.escalated();
        this.escalatedAt = s.escalatedAt();
        Map<String, String> refs = s.contextRefs().asMap();
        if (!refs.equals(this.contextRefs)) {
            this.contextRefs.clear();
            this.contextRefs.putAll(refs);
        }
        this.firstResponseDueAt = s.firstResponseDueAt();
        this.firstRespondedAt = s.firstRespondedAt();
        this.resolutionDueAt = s.resolutionDueAt();
        this.resolutionPeriodStartedAt = s.resolutionPeriodStartedAt();
        this.slaPausedSince = s.slaPausedSince();
        this.resolvedAt = s.resolvedAt();
        this.closedAt = s.closedAt();
        this.createdAt = s.createdAt();
        this.updatedAt = s.updatedAt();
        this.legacyId = s.legacyId();
        this.legacyNumber = s.legacyNumber();
    }

    Ticket toDomain() {
        return Ticket.restore(new Ticket.Snapshot(getId(), ticketNumber, subject, description, categoryCode, priority,
                priorityRank, status, requesterId, requesterRole, requesterReportedUrgent, assignedAgentId, escalated,
                escalatedAt, new ContextRefs(contextRefs), firstResponseDueAt, firstRespondedAt, resolutionDueAt,
                resolutionPeriodStartedAt, slaPausedSince, resolvedAt, closedAt, createdAt, updatedAt, legacyId,
                legacyNumber, version));
    }
}
