package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.Ticket;

import java.util.Optional;
import java.util.UUID;

/** Tickets, write side (table {@code support_tickets} and {@code ticket_context_refs}). */
public interface TicketStore {

    /** Loads a ticket with a row lock: concurrent commands on one ticket are serialized. */
    Optional<Ticket> lockForUpdate(UUID id);

    Optional<Ticket> find(UUID id);

    /** Inserts or updates; optimistic locking on the version. */
    void save(Ticket ticket);

    /** Next value of the ticket number sequence (BR-002). */
    long nextNumber();

    boolean existsByLegacyId(UUID legacyId);

    /** Keeps the stored rank of tickets in step when an administrator changes a priority's rank. */
    void updatePriorityRank(String priorityCode, int rank);
}
