package com.maplenou.backend.messaging.dto;

import com.maplenou.backend.messaging.Conversation;
import com.maplenou.backend.messaging.ConversationType;
import com.maplenou.backend.user.User;

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
        UUID deliveryId,
        UUID courierId,
        String courierName,
        Instant lastMessageAt,
        Instant createdAt
) {
    public static ConversationResponse from(Conversation c) {
        User courier = c.getDelivery() != null ? c.getDelivery().getAgent() : null;
        return new ConversationResponse(
                c.getId(),
                c.getType(),
                c.getShop() != null ? c.getShop().getId() : null,
                c.getShop() != null ? c.getShop().getName() : null,
                c.getBuyer() != null ? c.getBuyer().getId() : null,
                c.getBuyer() != null ? c.getBuyer().getFullName() : null,
                c.getSeller() != null ? c.getSeller().getId() : null,
                c.getDelivery() != null ? c.getDelivery().getId() : null,
                courier != null ? courier.getId() : null,
                courier != null ? courier.getFullName() : null,
                c.getLastMessageAt(),
                c.getCreatedAt()
        );
    }
}
