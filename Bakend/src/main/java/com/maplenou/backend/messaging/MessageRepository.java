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

    /** Messages non lus par conversation (messages reçus : envoyés par quelqu'un d'autre que [uid]). */
    @Query("""
            SELECT m.conversation.id, COUNT(m) FROM Message m
            WHERE m.conversation.id IN :ids AND m.readAt IS NULL AND m.sender.id <> :uid
            GROUP BY m.conversation.id
            """)
    java.util.List<Object[]> countUnreadByConversation(@org.springframework.data.repository.query.Param("ids") java.util.Collection<UUID> ids,
                                                       @org.springframework.data.repository.query.Param("uid") UUID uid);

    /** Total des messages non lus dans toutes les conversations de l'utilisateur (acheteur, vendeur ou livreur). */
    @Query("""
            SELECT COUNT(m) FROM Message m
            JOIN m.conversation c
            LEFT JOIN c.buyer b
            LEFT JOIN c.seller s
            LEFT JOIN c.delivery d
            LEFT JOIN d.agent a
            WHERE m.readAt IS NULL AND m.sender.id <> :uid
              AND (b.id = :uid OR s.id = :uid OR a.id = :uid)
            """)
    long countUnreadForUser(@org.springframework.data.repository.query.Param("uid") UUID uid);

    @Modifying
    @Query("""
            UPDATE Message m SET m.readAt = :now
            WHERE m.conversation.id = :conversationId AND m.sender.id != :readerId AND m.readAt IS NULL
            """)
    int markAllAsRead(UUID conversationId, UUID readerId, Instant now);
}
