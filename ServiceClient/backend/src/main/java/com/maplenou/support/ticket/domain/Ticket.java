package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Support ticket aggregate root — plain Java. Persistence adapters rebuild it with {@link #restore(Snapshot)} and
 * save {@link #snapshot()}; mutations go through the ticket application service, which checks the access policy
 * first and records history and events.
 */
public final class Ticket {

    /** Every field, for persistence adapters. */
    public record Snapshot(UUID id, String ticketNumber, String subject, String description, String categoryCode,
                           String priorityCode, int priorityRank, String statusCode, UUID requesterId,
                           Role requesterRole, boolean requesterReportedUrgent, UUID assignedAgentId, boolean escalated,
                           Instant escalatedAt, ContextRefs contextRefs, Instant firstResponseDueAt,
                           Instant firstRespondedAt, Instant resolutionDueAt, Instant resolutionPeriodStartedAt,
                           Instant slaPausedSince, Instant resolvedAt, Instant closedAt, Instant createdAt,
                           Instant updatedAt, UUID legacyId, String legacyNumber, long version) {
    }

    private final UUID id;
    private final String ticketNumber;
    private final String subject;
    private final String description;
    private String categoryCode;
    private String priorityCode;
    private int priorityRank;
    private String statusCode;
    private final UUID requesterId;
    private final Role requesterRole;
    private final boolean requesterReportedUrgent;
    private UUID assignedAgentId;
    private boolean escalated;
    private Instant escalatedAt;
    private ContextRefs contextRefs;
    private Instant firstResponseDueAt;
    private Instant firstRespondedAt;
    private Instant resolutionDueAt;
    private Instant resolutionPeriodStartedAt;
    private Instant slaPausedSince;
    private Instant resolvedAt;
    private Instant closedAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private UUID legacyId;
    private String legacyNumber;
    private final long version;

    private Ticket(Snapshot s) {
        this.id = Objects.requireNonNull(s.id());
        this.ticketNumber = s.ticketNumber();
        this.subject = s.subject();
        this.description = s.description();
        this.categoryCode = s.categoryCode();
        this.priorityCode = s.priorityCode();
        this.priorityRank = s.priorityRank();
        this.statusCode = s.statusCode();
        this.requesterId = s.requesterId();
        this.requesterRole = s.requesterRole();
        this.requesterReportedUrgent = s.requesterReportedUrgent();
        this.assignedAgentId = s.assignedAgentId();
        this.escalated = s.escalated();
        this.escalatedAt = s.escalatedAt();
        this.contextRefs = s.contextRefs() == null ? ContextRefs.NONE : s.contextRefs();
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
        this.version = s.version();
    }

    /** A new ticket: status NEW (BR-009), resolution period starting now. */
    public static Ticket open(UUID id, String number, String subject, String description, String categoryCode,
                              PriorityDefinition priority, String newStatusCode, UUID requesterId, Role requesterRole,
                              boolean reportedUrgent, ContextRefs refs, Instant now) {
        return new Ticket(new Snapshot(id, number, subject, description, categoryCode, priority.code(),
                priority.rank(), newStatusCode, requesterId, requesterRole, reportedUrgent, null, false, null, refs,
                null, null, null, now, null, null, null, now, now, null, null, 0L));
    }

    public static Ticket restore(Snapshot snapshot) {
        return new Ticket(snapshot);
    }

    public Snapshot snapshot() {
        return new Snapshot(id, ticketNumber, subject, description, categoryCode, priorityCode, priorityRank,
                statusCode, requesterId, requesterRole, requesterReportedUrgent, assignedAgentId, escalated,
                escalatedAt, contextRefs, firstResponseDueAt, firstRespondedAt, resolutionDueAt,
                resolutionPeriodStartedAt, slaPausedSince, resolvedAt, closedAt, createdAt, updatedAt, legacyId,
                legacyNumber, version);
    }

    // ------------------------------------------------------------------ behaviour

    public void setSlaTargets(Instant firstResponseDue, Instant resolutionDue) {
        this.firstResponseDueAt = firstResponseDue;
        this.resolutionDueAt = resolutionDue;
    }

    /** A new resolution period after an approved reopening (BR-055). */
    public void startResolutionPeriod(Instant start, Instant due) {
        this.resolutionPeriodStartedAt = start;
        this.resolutionDueAt = due;
        this.slaPausedSince = null;
    }

    /** Changes the status; resolution/closure timestamps follow the status kind. */
    public void changeStatus(String target, StatusKind kind, Instant now) {
        this.statusCode = target;
        if (kind == StatusKind.RESOLVED) {
            this.resolvedAt = now;
        } else if (kind == StatusKind.CLOSED) {
            this.closedAt = now;
            if (this.resolvedAt == null) {
                this.resolvedAt = now;
            }
        } else {
            this.resolvedAt = null;
        }
        touch(now);
    }

    public void changePriority(PriorityDefinition priority, Instant now) {
        this.priorityCode = priority.code();
        this.priorityRank = priority.rank();
        touch(now);
    }

    public void changeCategory(String categoryCode, Instant now) {
        this.categoryCode = categoryCode;
        touch(now);
    }

    public void assignTo(UUID agentId, Instant now) {
        this.assignedAgentId = agentId;
        touch(now);
    }

    /** First qualifying support response (FR-016); later answers do not move it. */
    public void markFirstResponse(Instant at) {
        if (this.firstRespondedAt == null) {
            this.firstRespondedAt = at;
        }
    }

    public void escalate(Instant now) {
        this.escalated = true;
        this.escalatedAt = now;
        touch(now);
    }

    /** The resolution clock stops (status that waits for the requester or a third party, DEC-CS11-03). */
    public void pauseResolutionClock(Instant now) {
        if (slaPausedSince == null) {
            slaPausedSince = now;
        }
    }

    /** The resolution clock runs again; {@code newDue} already includes the paused time. */
    public void resumeResolutionClock(Instant newDue) {
        this.resolutionDueAt = newDue;
        this.slaPausedSince = null;
    }

    public void linkContext(ContextRefs refs) {
        this.contextRefs = refs == null ? ContextRefs.NONE : refs;
    }

    public void markImported(UUID legacyId, String legacyNumber) {
        this.legacyId = legacyId;
        this.legacyNumber = legacyNumber;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }

    // ------------------------------------------------------------------ queries

    public boolean isAssignedTo(UUID userId) {
        return assignedAgentId != null && assignedAgentId.equals(userId);
    }

    public boolean isSlaPaused() {
        return slaPausedSince != null;
    }

    public UUID id() {
        return id;
    }

    public String ticketNumber() {
        return ticketNumber;
    }

    public String subject() {
        return subject;
    }

    public String description() {
        return description;
    }

    public String categoryCode() {
        return categoryCode;
    }

    public String priorityCode() {
        return priorityCode;
    }

    public int priorityRank() {
        return priorityRank;
    }

    public String statusCode() {
        return statusCode;
    }

    public UUID requesterId() {
        return requesterId;
    }

    public Role requesterRole() {
        return requesterRole;
    }

    public boolean isRequesterReportedUrgent() {
        return requesterReportedUrgent;
    }

    public UUID assignedAgentId() {
        return assignedAgentId;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public Instant escalatedAt() {
        return escalatedAt;
    }

    public ContextRefs contextRefs() {
        return contextRefs;
    }

    public Instant firstResponseDueAt() {
        return firstResponseDueAt;
    }

    public Instant firstRespondedAt() {
        return firstRespondedAt;
    }

    public Instant resolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant resolutionPeriodStartedAt() {
        return resolutionPeriodStartedAt;
    }

    public Instant slaPausedSince() {
        return slaPausedSince;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public UUID legacyId() {
        return legacyId;
    }

    public String legacyNumber() {
        return legacyNumber;
    }

    public long version() {
        return version;
    }
}
