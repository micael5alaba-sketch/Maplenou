package com.maplenou.backend.messaging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByTypeAndShopIdAndBuyerId(ConversationType type, UUID shopId, UUID buyerId);

    Optional<Conversation> findByTypeAndBuyerId(ConversationType type, UUID buyerId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.buyer.id = :userId OR c.seller.id = :userId
            ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC
            """)
    Page<Conversation> findAllForUser(UUID userId, Pageable pageable);

    Page<Conversation> findByTypeOrderByLastMessageAtDesc(ConversationType type, Pageable pageable);
}
