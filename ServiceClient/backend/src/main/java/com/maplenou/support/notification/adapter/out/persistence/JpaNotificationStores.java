package com.maplenou.support.notification.adapter.out.persistence;

import com.maplenou.support.notification.application.port.DeviceTokenStore;
import com.maplenou.support.notification.application.port.NotificationStore;
import com.maplenou.support.notification.application.port.TemplateStore;
import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.notification.domain.MessageTemplate;
import com.maplenou.support.notification.domain.Notification;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaNotificationStores implements NotificationStore, DeviceTokenStore, TemplateStore {

    private final NotificationJpaRepository notifications;
    private final DeviceTokenJpaRepository tokens;
    private final TemplateJpaRepository templates;

    JpaNotificationStores(NotificationJpaRepository notifications, DeviceTokenJpaRepository tokens,
                          TemplateJpaRepository templates) {
        this.notifications = notifications;
        this.tokens = tokens;
        this.templates = templates;
    }

    @Override
    public void save(Notification n) {
        NotificationEntity e = notifications.findById(n.id()).orElse(null);
        if (e == null) {
            notifications.save(new NotificationEntity(n));
        } else {
            e.copyFrom(n);
        }
    }

    @Override
    public Optional<Notification> find(UUID id) {
        return notifications.findById(id).map(NotificationEntity::toDomain);
    }

    @Override
    public PageResult<Notification> forRecipient(UUID recipientId, boolean unreadOnly, PageQuery page) {
        PageRequest pr = PageRequest.of(page.page(), page.size());
        Page<NotificationEntity> result = unreadOnly
                ? notifications.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId, pr)
                : notifications.findByRecipientIdOrderByCreatedAtDesc(recipientId, pr);
        return new PageResult<>(result.getContent().stream().map(NotificationEntity::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public long countUnread(UUID recipientId) {
        return notifications.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Override
    public int markAllRead(UUID recipientId, Instant now) {
        return notifications.markAllRead(recipientId, now);
    }

    @Override
    public List<UUID> lockDuePush(Instant now, int limit) {
        return notifications.lockDuePush(now, limit);
    }

    @Override
    public List<Notification> forTicket(UUID ticketId) {
        return notifications.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(NotificationEntity::toDomain).toList();
    }

    // ------------------------------------------------------------------ device tokens

    @Override
    public List<DeviceToken> forUser(UUID userId) {
        return tokens.findByUserId(userId).stream().map(DeviceTokenEntity::toDomain).toList();
    }

    @Override
    public Optional<DeviceToken> findByToken(String token) {
        return tokens.findByToken(token).map(DeviceTokenEntity::toDomain);
    }

    @Override
    public void save(DeviceToken token) {
        DeviceTokenEntity e = tokens.findById(token.id()).orElse(null);
        if (e == null) {
            tokens.save(new DeviceTokenEntity(token));
        } else {
            e.copyFrom(token);
        }
    }

    @Override
    public void deleteByToken(String token) {
        tokens.deleteByToken(token);
    }

    // ------------------------------------------------------------------ templates

    @Override
    public List<MessageTemplate> allTemplates() {
        return templates.findAll().stream().map(TemplateEntity::toDomain).toList();
    }

    @Override
    public void save(MessageTemplate template) {
        TemplateEntity e = templates.findById(new TemplateId(template.type(), template.language())).orElse(null);
        if (e == null) {
            templates.save(new TemplateEntity(template));
        } else {
            e.copyFrom(template);
        }
    }
}
