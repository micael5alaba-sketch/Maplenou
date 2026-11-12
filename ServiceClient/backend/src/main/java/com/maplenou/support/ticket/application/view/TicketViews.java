package com.maplenou.support.ticket.application.view;

import com.maplenou.support.shared.Role;
import com.maplenou.support.sla.domain.SlaState;
import com.maplenou.support.ticket.domain.AiSuggestion;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.ExternalAction;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.ReopenRequest;
import com.maplenou.support.ticket.domain.TicketAction;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read models returned by the ticket use cases. Support-only fields are {@code null} for requesters. */
public final class TicketViews {

    private TicketViews() {
    }

    public record PersonView(UUID id, String name, Role role) {
    }

    public record CategoryView(UUID id, String code, String name, String nameEn, String description,
                               String descriptionEn, int sortOrder, boolean active) {
        public static CategoryView of(Category c) {
            return c == null ? null : new CategoryView(c.id(), c.code(), c.name(), c.nameEn(), c.description(),
                    c.descriptionEn(), c.sortOrder(), c.active());
        }
    }

    /** Maplenou references (kept by name for the clients) and every reference by type. */
    public record References(String orderRef, String paymentRef, String deliveryRef, String sellerRef,
                             String refundRef, Map<String, String> all) {
    }

    public record SlaClock(Instant startedAt, Instant dueAt, Instant completedAt, SlaState state) {
    }

    public record SlaPeriodView(int periodNumber, String priority, Instant startedAt, Instant dueAt, Instant endedAt,
                                String endReason, long pausedMinutes) {
    }

    public record SlaView(SlaClock firstResponse, SlaClock resolution, SlaState overall, Instant pausedSince,
                          List<SlaPeriodView> periods) {
    }

    public record ReopenView(UUID id, ReopenRequest.Status status, String reason, Instant requestedAt,
                             Instant decidedAt, String decisionNote) {
        public static ReopenView of(ReopenRequest r) {
            return r == null ? null : new ReopenView(r.id(), r.status(), r.reason(), r.createdAt(), r.decidedAt(),
                    r.decisionNote());
        }
    }

    public record SatisfactionView(int rating, String comment, Instant ratedAt) {
    }

    public record AiSuggestionView(String categoryCode, String priorityCode, double confidence, String reason,
                                   Instant createdAt, AiSuggestion.Decision decision) {
    }

    public record ExternalActionView(UUID id, ExternalAction.Type type, String targetRef, boolean succeeded,
                                     String resultRef, String error, PersonView requestedBy, Instant createdAt) {
    }

    public record TicketSummary(UUID id, String ticketNumber, String subject, CategoryView category,
                                String priority, String status, StatusKind statusKind, PersonView requester,
                                PersonView assignedAgent, boolean escalated, SlaState slaState,
                                String orderRef, String deliveryRef, Instant createdAt, Instant updatedAt) {
    }

    public record TicketDetail(UUID id, String ticketNumber, String subject, String description,
                               CategoryView category, String priority, String status, StatusKind statusKind,
                               PersonView requester, PersonView assignedAgent, boolean requesterReportedUrgent,
                               boolean escalated, Instant escalatedAt, References references, SlaView sla,
                               ReopenView pendingReopenRequest, ReopenView lastReopenDecision,
                               Set<TicketAction> allowedActions, Set<String> allowedStatusTargets,
                               Instant createdAt, Instant updatedAt, Instant resolvedAt, Instant closedAt,
                               long version, String legacyNumber, SatisfactionView satisfaction,
                               AiSuggestionView aiSuggestion, List<ExternalActionView> externalActions) {
    }

    public record AttachmentView(UUID id, String filename, String contentType, long sizeBytes, Instant createdAt) {
    }

    public record MessageView(UUID id, PersonView author, MessageVisibility visibility, String body,
                              Instant createdAt, List<AttachmentView> attachments) {
    }

    public record HistoryView(UUID id, HistoryAction action, PersonView actor, String oldValue, String newValue,
                              String details, Instant createdAt) {
    }
}
