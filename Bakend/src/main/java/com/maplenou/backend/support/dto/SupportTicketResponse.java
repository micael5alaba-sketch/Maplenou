package com.maplenou.backend.support.dto;

import com.maplenou.backend.support.SupportTicket;
import com.maplenou.backend.support.SupportTicketStatus;

import java.time.Instant;
import java.util.UUID;

public record SupportTicketResponse(
        UUID id,
        String ticketNumber,
        String requesterName,
        String category,
        String subject,
        String description,
        UUID relatedDeliveryId,
        String photoUrl,
        SupportTicketStatus status,
        UUID conversationId,
        String resolutionNotes,
        Instant resolvedAt,
        Instant createdAt
) {
    public static SupportTicketResponse from(SupportTicket t) {
        return new SupportTicketResponse(
                t.getId(),
                t.getTicketNumber(),
                t.getRequester().getFullName(),
                t.getCategory(),
                t.getSubject(),
                t.getDescription(),
                t.getRelatedDelivery() != null ? t.getRelatedDelivery().getId() : null,
                t.getPhotoUrl(),
                t.getStatus(),
                t.getConversation() != null ? t.getConversation().getId() : null,
                t.getResolutionNotes(),
                t.getResolvedAt(),
                t.getCreatedAt()
        );
    }
}
