package com.maplenou.support.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Category and priority proposed by the assistant for a new ticket (DEC-CS11-08). Advisory only: an agent applies
 * or dismisses it; nothing changes on the ticket by itself.
 */
public record AiSuggestion(UUID ticketId, String categoryCode, String priorityCode, double confidence, String reason,
                           String model, Instant createdAt, Decision decision, UUID decidedBy, Instant decidedAt) {

    public enum Decision { APPLIED, DISMISSED }

    public AiSuggestion decide(Decision d, UUID by, Instant at) {
        return new AiSuggestion(ticketId, categoryCode, priorityCode, confidence, reason, model, createdAt, d, by, at);
    }
}
