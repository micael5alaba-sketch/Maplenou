package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Role;

import java.time.Instant;
import java.util.UUID;

/** A message of the conversation or an internal note; immutable once written. */
public record TicketMessage(UUID id, UUID ticketId, UUID authorId, Role authorRole, MessageVisibility visibility,
                            String body, String clientMessageId, Instant createdAt) {
}
