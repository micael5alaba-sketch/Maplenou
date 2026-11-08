package com.maplenou.support.ticket.domain.event;

import com.maplenou.support.shared.event.DomainEvent;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the ticket use cases in the ticket transaction (outbox); consumed by notifications after commit.
 *
 * @param statusKind meaning of {@code statusCode}, so consumers need not know the configured statuses
 * @param detail     APPROVED/REJECTED for REOPEN_DECIDED
 * @param actorId    null for automatic changes
 */
public record TicketEvent(UUID eventId, TicketEventType type, UUID ticketId, String ticketNumber, String subject,
                          UUID requesterId, UUID assignedAgentId, UUID actorId, String statusCode,
                          StatusKind statusKind, String detail, Instant occurredAt) implements DomainEvent {
}
