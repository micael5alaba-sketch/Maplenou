package com.maplenou.support.platform.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One domain event waiting for delivery, or delivered (table {@code outbox_events}). */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    private UUID id;
    @Column(name = "event_type", nullable = false, updatable = false)
    private String eventType;
    @Column(nullable = false, updatable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;
    @Column(name = "last_error")
    private String lastError;
    @Column(nullable = false)
    private boolean failed;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(UUID id, String eventType, String payload, Instant now) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    void published(Instant now) {
        this.publishedAt = now;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    void failedAttempt(String error, Instant now, int maxAttempts) {
        this.attempts++;
        this.lastError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 480));
        if (attempts >= maxAttempts) {
            this.failed = true;
            this.nextAttemptAt = null;
        } else {
            this.nextAttemptAt = now.plusSeconds((long) Math.min(3600, Math.pow(2, attempts) * 5));
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public boolean isFailed() {
        return failed;
    }
}
