package com.maplenou.support.ticket.application.port;

import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import com.maplenou.support.ticket.application.TicketSearchCriteria;
import com.maplenou.support.ticket.domain.Ticket;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tickets, read side: search, lists and counts for dashboards and reports. */
public interface TicketSearch {

    /** Sort fields: createdAt, updatedAt, priorityRank, ticketNumber, status. */
    record Sort(String field, boolean ascending) {
    }

    PageResult<Ticket> search(TicketSearchCriteria criteria, UUID currentUserId, PageQuery page, Sort sort);

    /** Every matching ticket up to {@code limit} (CSV export), newest first. */
    List<Ticket> list(TicketSearchCriteria criteria, UUID currentUserId, int limit);

    List<Ticket> createdBetween(Instant from, Instant to);

    /** Resolved tickets whose resolution is older than {@code before}, for the automatic closure. */
    List<UUID> resolvedBefore(Collection<String> resolvedStatuses, Instant before, int limit);

    long countByRequester(UUID requesterId);

    long countWithStatus(Collection<String> statuses);

    long countUnassigned(Collection<String> statuses);

    long countAssignedTo(UUID agentId, Collection<String> statuses);

    long countEscalated(Collection<String> statuses);

    long countResolvedSince(Instant since);

    /** Open tickets past a deadline (first response not given in time, or resolution clock running and late). */
    long countBreached(Collection<String> openStatuses, Instant now);

    Map<String, Long> countByPriority(Collection<String> statuses);

    /** Category code → count. */
    Map<String, Long> countByCategory(Collection<String> statuses);

    Map<UUID, Long> openLoadByAgent(Collection<String> openStatuses);
}
