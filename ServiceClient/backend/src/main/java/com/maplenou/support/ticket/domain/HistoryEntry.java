package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Role;

import java.time.Instant;
import java.util.UUID;

/** Append-only audit entry (BR-017, BR-018). {@code actorId} is null for automatic changes. */
public record HistoryEntry(UUID id, UUID ticketId, UUID actorId, Role actorRole, HistoryAction action,
                           String oldValue, String newValue, String details, boolean requesterVisible,
                           Instant createdAt) {
}
