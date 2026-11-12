package com.maplenou.support.notification.adapter.out.persistence;

import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.notification.domain.MessageTemplate;
import com.maplenou.support.notification.domain.Notification;
import com.maplenou.support.notification.domain.NotificationType;
import com.maplenou.support.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
class NotificationEntity extends BaseEntity {
    @Column(name = "recipient_id", nullable = false, updatable = false)
    UUID recipientId;
    @Column(name = "ticket_id", updatable = false)
    UUID ticketId;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false)
    NotificationType type;
    @Column(nullable = false, updatable = false)
    String title;
    @Column(nullable = false, updatable = false)
    String body;
    @Column(name = "ticket_number", updatable = false)
    String ticketNumber;
    @Column(updatable = false)
    String detail;
    @Column(name = "read_at")
    Instant readAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "push_status", nullable = false)
    Notification.PushStatus pushStatus;
    @Column(name = "push_attempts", nullable = false)
    short pushAttempts;
    @Column(name = "push_next_attempt_at")
    Instant pushNextAttemptAt;
    @Column(name = "push_last_error")
    String pushLastError;
    @Column(name = "push_sent_at")
    Instant pushSentAt;

    protected NotificationEntity() {
    }

    NotificationEntity(Notification n) {
        super(n.id());
        this.recipientId = n.recipientId();
        this.ticketId = n.ticketId();
        this.type = n.type();
        this.title = n.title();
        this.body = n.body();
        this.ticketNumber = n.ticketNumber();
        this.detail = n.detail();
        this.createdAt = n.createdAt();
        copyFrom(n);
    }

    void copyFrom(Notification n) {
        this.readAt = n.readAt();
        this.pushStatus = n.pushStatus();
        this.pushAttempts = (short) n.pushAttempts();
        this.pushNextAttemptAt = n.pushNextAttemptAt();
        this.pushLastError = n.pushLastError();
        this.pushSentAt = n.pushSentAt();
    }

    Notification toDomain() {
        return new Notification(getId(), recipientId, ticketId, type, title, body, ticketNumber, detail, readAt,
                createdAt, pushStatus, pushAttempts, pushNextAttemptAt, pushLastError, pushSentAt);
    }
}

@Entity
@Table(name = "device_tokens")
class DeviceTokenEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    UUID userId;
    @Column(nullable = false, unique = true)
    String token;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    DeviceToken.Platform platform;
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;
    @Column(name = "last_seen_at", nullable = false)
    Instant lastSeenAt;

    protected DeviceTokenEntity() {
    }

    DeviceTokenEntity(DeviceToken t) {
        super(t.id());
        this.token = t.token();
        this.createdAt = t.createdAt();
        copyFrom(t);
    }

    void copyFrom(DeviceToken t) {
        this.userId = t.userId();
        this.platform = t.platform();
        this.lastSeenAt = t.lastSeenAt();
    }

    DeviceToken toDomain() {
        return new DeviceToken(getId(), userId, token, platform, createdAt, lastSeenAt);
    }
}

@Embeddable
class TemplateId implements Serializable {
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    NotificationType type;
    String language;

    protected TemplateId() {
    }

    TemplateId(NotificationType type, String language) {
        this.type = type;
        this.language = language;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TemplateId t && type == t.type && Objects.equals(language, t.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, language);
    }
}

@Entity
@Table(name = "notification_templates")
class TemplateEntity {
    @EmbeddedId
    TemplateId id;
    @Column(nullable = false)
    String title;
    @Column(nullable = false)
    String body;

    protected TemplateEntity() {
    }

    TemplateEntity(MessageTemplate t) {
        this.id = new TemplateId(t.type(), t.language());
        copyFrom(t);
    }

    void copyFrom(MessageTemplate t) {
        this.title = t.title();
        this.body = t.body();
    }

    MessageTemplate toDomain() {
        return new MessageTemplate(id.type, id.language, title, body);
    }
}
