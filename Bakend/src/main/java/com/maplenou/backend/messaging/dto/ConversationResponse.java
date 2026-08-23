package com.maplenou.backend.messaging.dto;

import com.maplenou.backend.messaging.Conversation;
import com.maplenou.backend.messaging.ConversationType;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        ConversationType type,
        UUID shopId,
        String shopName,
        UUID buyerId,
        String buyerName,
        UUID sellerId,
        Instant lastMessageAt,
        Instant createdAt
) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(
                c.getId(),
                c.getType(),
                c.getShop() != null ? c.getShop().getId() : null,
                c.getShop() != null ? c.getShop().getName() : null,
                c.getBuyer().getId(),
                c.getBuyer().getFullName(),
                c.getSeller() != null ? c.getSeller().getId() : null,
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
