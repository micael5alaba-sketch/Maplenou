package com.maplenou.backend.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.conversation.id = :conversationId AND m.sender.id != :readerId AND m.readAt IS NULL
            """)
    int markAllAsRead(UUID conversationId, UUID readerId, Instant now);
}
