package com.maplenou.support.notification.application.port;

import com.maplenou.support.notification.domain.Notification;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationStore {

    void save(Notification notification);

    Optional<Notification> find(UUID id);

    PageResult<Notification> forRecipient(UUID recipientId, boolean unreadOnly, PageQuery page);

    long countUnread(UUID recipientId);

    int markAllRead(UUID recipientId, Instant now);

    /** Due push deliveries, locked so several instances never send the same notification twice. */
    List<UUID> lockDuePush(Instant now, int limit);

    List<Notification> forTicket(UUID ticketId);
}
