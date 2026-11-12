package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Role;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Search and filter criteria for the ticket list (FR-014, docs/architecture/api-specification.md §Search).
 *
 * @param q            free text: matches the ticket number, the former number or the subject (case-insensitive)
 * @param assignee     an agent id, {@code me} or {@code unassigned}
 * @param openStatuses statuses counted as open (filled by the use case from the workflow)
 * @param slaBreached  only tickets whose first-response or resolution target has passed
 * @param zone         time zone of {@code createdFrom}/{@code createdTo}
 */
public record TicketSearchCriteria(
        String q,
        Set<String> statuses,
        Set<String> priorities,
        Set<String> categoryCodes,
        String assignee,
        UUID requesterId,
        Role requesterRole,
        Boolean escalated,
        Boolean slaBreached,
        Boolean openOnly,
        LocalDate createdFrom,
        LocalDate createdTo,
        Set<String> openStatuses,
        ZoneId zone) {

    public static TicketSearchCriteria openTickets() {
        return new TicketSearchCriteria(null, null, null, null, null, null, null, null, null, true, null, null, null,
                null);
    }

    public TicketSearchCriteria withRequester(UUID requester) {
        return new TicketSearchCriteria(q, statuses, priorities, categoryCodes, null, requester, null, null, null,
                openOnly, createdFrom, createdTo, openStatuses, zone);
    }

    public TicketSearchCriteria withContext(Set<String> open, ZoneId zoneId) {
        return new TicketSearchCriteria(q, statuses, priorities, categoryCodes, assignee, requesterId, requesterRole,
                escalated, slaBreached, openOnly, createdFrom, createdTo, open, zoneId);
    }
}
