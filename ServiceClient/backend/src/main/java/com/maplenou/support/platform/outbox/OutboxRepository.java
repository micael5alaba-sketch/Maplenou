package com.maplenou.support.platform.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /** Events due for delivery, oldest first. */
    @Query(value = """
            select id from outbox_events
            where published_at is null and not failed and next_attempt_at <= :now
            order by created_at
            limit :limit
            """, nativeQuery = true)
    List<UUID> findDue(@Param("now") Instant now, @Param("limit") int limit);

    /** Locks one undelivered event; another instance delivering it at the same time skips it. */
    @Query(value = "select * from outbox_events where id = :id and published_at is null for update skip locked",
            nativeQuery = true)
    Optional<OutboxEventEntity> lockUndelivered(@Param("id") UUID id);

    @Query(value = "select count(*) from outbox_events where published_at is null and not failed", nativeQuery = true)
    long countPending();

    @Query(value = "select count(*) from outbox_events where failed", nativeQuery = true)
    long countFailed();
}
