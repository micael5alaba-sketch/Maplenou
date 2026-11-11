package com.maplenou.support.notification.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * In-app notification plus its push-delivery state. Created from a ticket event; push is delivered later with
 * retries (BR-020).
 */
public final class Notification {

    public enum PushStatus { PENDING, SENT, FAILED, SKIPPED }

    private final UUID id;
    private final UUID recipientId;
    private final UUID ticketId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String ticketNumber;
    private final String detail;
    private Instant readAt;
    private final Instant createdAt;
    private PushStatus pushStatus;
    private int pushAttempts;
    private Instant pushNextAttemptAt;
    private String pushLastError;
    private Instant pushSentAt;

    public Notification(UUID id, UUID recipientId, UUID ticketId, NotificationType type, String title, String body,
                        String ticketNumber, String detail, Instant readAt, Instant createdAt, PushStatus pushStatus,
                        int pushAttempts, Instant pushNextAttemptAt, String pushLastError, Instant pushSentAt) {
        this.id = id;
        this.recipientId = recipientId;
        this.ticketId = ticketId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.ticketNumber = ticketNumber;
        this.detail = detail;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.pushStatus = pushStatus;
        this.pushAttempts = pushAttempts;
        this.pushNextAttemptAt = pushNextAttemptAt;
        this.pushLastError = pushLastError;
        this.pushSentAt = pushSentAt;
    }

    public static Notification create(UUID id, UUID recipientId, UUID ticketId, NotificationType type, String title,
                                      String body, String ticketNumber, String detail, Instant now) {
        return new Notification(id, recipientId, ticketId, type, title, body, ticketNumber, detail, null, now,
                PushStatus.PENDING, 0, now, null, null);
    }

    public void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public void pushSent(Instant now) {
        pushStatus = PushStatus.SENT;
        pushSentAt = now;
        pushAttempts++;
        pushNextAttemptAt = null;
        pushLastError = null;
    }

    public void pushSkipped() {
        pushStatus = PushStatus.SKIPPED;
        pushNextAttemptAt = null;
    }

    /** Retries with an exponential backoff (30 s, 60 s, 120 s…) until {@code maxAttempts}. */
    public void pushFailed(String error, Instant now, int maxAttempts) {
        pushAttempts++;
        pushLastError = error == null ? "unknown" : error.substring(0, Math.min(error.length(), 480));
        if (pushAttempts >= maxAttempts) {
            pushStatus = PushStatus.FAILED;
            pushNextAttemptAt = null;
        } else {
            long backoffSeconds = (long) Math.pow(2, pushAttempts) * 15;
            pushNextAttemptAt = now.plusSeconds(backoffSeconds);
        }
    }

    public UUID id() {
        return id;
    }

    public UUID recipientId() {
        return recipientId;
    }

    public UUID ticketId() {
        return ticketId;
    }

    public NotificationType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public String ticketNumber() {
        return ticketNumber;
    }

    public String detail() {
        return detail;
    }

    public Instant readAt() {
        return readAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public PushStatus pushStatus() {
        return pushStatus;
    }

    public int pushAttempts() {
        return pushAttempts;
    }

    public Instant pushNextAttemptAt() {
        return pushNextAttemptAt;
    }

    public String pushLastError() {
        return pushLastError;
    }

    public Instant pushSentAt() {
        return pushSentAt;
    }
}
