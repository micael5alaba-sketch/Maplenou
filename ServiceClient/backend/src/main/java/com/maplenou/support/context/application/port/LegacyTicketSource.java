package com.maplenou.support.context.application.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The tickets of the core's former support module (Bakend {@code GET /internal/v1/support-tickets}), for the
 * one-time import (DEC-CS10-17).
 */
public interface LegacyTicketSource {

    record LegacyTicket(UUID id, String ticketNumber, UUID requesterId, String requesterName, String requesterPhone,
                        String requesterEmail, String requesterRole, String category, String subject,
                        String description, String relatedDeliveryRef, String photoUrl, String status,
                        String resolutionNotes, Instant resolvedAt, Instant createdAt) {
    }

    record LegacyTicketPage(List<LegacyTicket> items, int page, int size, long totalItems, boolean last) {
    }

    LegacyTicketPage page(int page, int size);
}
