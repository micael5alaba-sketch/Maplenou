package com.maplenou.support.notification.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<NotificationEntity> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    List<NotificationEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    @Modifying
    @Query("update NotificationEntity n set n.readAt = :now where n.recipientId = :recipient and n.readAt is null")
    int markAllRead(@Param("recipient") UUID recipient, @Param("now") Instant now);

    @Query(value = """
            select id from notifications
            where push_status = 'PENDING' and push_next_attempt_at <= :now
            order by push_next_attempt_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<UUID> lockDuePush(@Param("now") Instant now, @Param("limit") int limit);
}

interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenEntity, UUID> {

    List<DeviceTokenEntity> findByUserId(UUID userId);

    Optional<DeviceTokenEntity> findByToken(String token);

    @Modifying
    @Query("delete from DeviceTokenEntity d where d.token = :token")
    void deleteByToken(@Param("token") String token);
}

interface TemplateJpaRepository extends JpaRepository<TemplateEntity, TemplateId> {
}
