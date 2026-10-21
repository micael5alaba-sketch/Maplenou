package com.maplenou.backend.notification.dto;

import com.maplenou.backend.notification.AppNotification;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String title,
        String body,
        String type,
        String referenceId,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(AppNotification n) {
        return new NotificationResponse(
                n.getId(), n.getTitle(), n.getBody(), n.getType(), n.getReferenceId(),
                n.getReadAt() != null, n.getCreatedAt()
        );
    }
}
