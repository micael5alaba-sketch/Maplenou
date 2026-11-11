package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.domain.AiSuggestion;
import com.maplenou.support.ticket.domain.CannedReply;
import com.maplenou.support.ticket.domain.ExternalAction;
import com.maplenou.support.ticket.domain.Satisfaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_satisfaction")
class SatisfactionEntity {
    @Id
    @Column(name = "ticket_id")
    UUID ticketId;
    @Column(nullable = false)
    short rating;
    String comment;
    @Column(name = "rated_by", nullable = false)
    UUID ratedBy;
    @Column(name = "rated_at", nullable = false)
    Instant ratedAt;

    protected SatisfactionEntity() {
    }

    SatisfactionEntity(Satisfaction s) {
        this.ticketId = s.ticketId();
        this.rating = (short) s.rating();
        this.comment = s.comment();
        this.ratedBy = s.ratedBy();
        this.ratedAt = s.ratedAt();
    }

    Satisfaction toDomain() {
        return new Satisfaction(ticketId, rating, comment, ratedBy, ratedAt);
    }
}

@Entity
@Table(name = "canned_replies")
class CannedReplyEntity {
    @Id
    UUID id;
    @Column(nullable = false)
    String title;
    @Column(nullable = false)
    String body;
    @Column(nullable = false)
    String language;
    @Column(name = "category_code")
    String categoryCode;
    @Column(nullable = false)
    boolean active;
    @Column(name = "updated_by")
    UUID updatedBy;
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    protected CannedReplyEntity() {
    }

    CannedReplyEntity(CannedReply r) {
        this.id = r.id();
        copyFrom(r);
    }

    void copyFrom(CannedReply r) {
        this.title = r.title();
        this.body = r.body();
        this.language = r.language();
        this.categoryCode = r.categoryCode();
        this.active = r.active();
        this.updatedBy = r.updatedBy();
        this.updatedAt = r.updatedAt();
    }

    CannedReply toDomain() {
        return new CannedReply(id, title, body, language, categoryCode, active, updatedBy, updatedAt);
    }
}

@Entity
@Table(name = "ticket_ai_suggestions")
class AiSuggestionEntity {
    @Id
    @Column(name = "ticket_id")
    UUID ticketId;
    @Column(name = "category_code", nullable = false)
    String categoryCode;
    @Column(name = "priority_code", nullable = false)
    String priorityCode;
    @Column(nullable = false, precision = 4, scale = 3)
    BigDecimal confidence;
    String reason;
    String model;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;
    @Enumerated(EnumType.STRING)
    AiSuggestion.Decision decision;
    @Column(name = "decided_by")
    UUID decidedBy;
    @Column(name = "decided_at")
    Instant decidedAt;

    protected AiSuggestionEntity() {
    }

    AiSuggestionEntity(AiSuggestion s) {
        this.ticketId = s.ticketId();
        copyFrom(s);
    }

    void copyFrom(AiSuggestion s) {
        this.categoryCode = s.categoryCode();
        this.priorityCode = s.priorityCode();
        this.confidence = BigDecimal.valueOf(s.confidence()).setScale(3, RoundingMode.HALF_UP);
        this.reason = s.reason() == null ? null : s.reason().substring(0, Math.min(500, s.reason().length()));
        this.model = s.model() == null ? null : s.model().substring(0, Math.min(80, s.model().length()));
        this.createdAt = s.createdAt();
        this.decision = s.decision();
        this.decidedBy = s.decidedBy();
        this.decidedAt = s.decidedAt();
    }

    AiSuggestion toDomain() {
        return new AiSuggestion(ticketId, categoryCode, priorityCode, confidence.doubleValue(), reason, model, createdAt,
                decision, decidedBy, decidedAt);
    }
}

@Entity
@Table(name = "ticket_external_actions")
class ExternalActionEntity {
    @Id
    UUID id;
    @Column(name = "ticket_id", nullable = false)
    UUID ticketId;
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    ExternalAction.Type type;
    @Column(name = "target_ref", nullable = false)
    String targetRef;
    @Column(nullable = false)
    boolean succeeded;
    @Column(name = "result_ref")
    String resultRef;
    String error;
    @Column(name = "requested_by", nullable = false)
    UUID requestedBy;
    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    protected ExternalActionEntity() {
    }

    ExternalActionEntity(ExternalAction a) {
        this.id = a.id();
        this.ticketId = a.ticketId();
        this.type = a.type();
        this.targetRef = a.targetRef();
        this.succeeded = a.succeeded();
        this.resultRef = a.resultRef();
        this.error = a.error();
        this.requestedBy = a.requestedBy();
        this.createdAt = a.createdAt();
    }

    ExternalAction toDomain() {
        return new ExternalAction(id, ticketId, type, targetRef, succeeded, resultRef, error, requestedBy, createdAt);
    }
}
