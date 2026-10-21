package com.maplenou.backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<AppNotification, UUID> {

    Page<AppNotification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    @Modifying
    @Query("UPDATE AppNotification n SET n.readAt = :now WHERE n.recipient.id = :recipientId AND n.readAt IS NULL")
    void markAllAsRead(@Param("recipientId") UUID recipientId, @Param("now") Instant now);
}
