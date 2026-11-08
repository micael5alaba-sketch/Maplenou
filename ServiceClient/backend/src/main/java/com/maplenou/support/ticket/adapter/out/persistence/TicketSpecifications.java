package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.TicketSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Criteria API translation of the ticket search (FR-014). */
final class TicketSpecifications {

    private TicketSpecifications() {
    }

    static Specification<TicketEntity> of(TicketSearchCriteria c, UUID currentUserId, Instant now) {
        ZoneId zone = c.zone() == null ? ZoneId.of("UTC") : c.zone();
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            String q = c.q();
            if (q != null && q.trim().matches("(?i)TKT-\\d{4}-\\d+")) {
                // a complete ticket number is an exact lookup — on the number, or on the old number of a ticket
                // imported from Bakend's former support, which uses the same prefix (TKT-YYYY-NNNNNN, 6 digits)
                String number = q.trim().toUpperCase(Locale.ROOT);
                p.add(cb.or(cb.equal(root.get("ticketNumber"), number), cb.equal(root.get("legacyNumber"), number)));
            } else if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("ticketNumber")), like, '\\'),
                        // number from the former Maplenou support (imported tickets)
                        cb.like(cb.lower(cb.coalesce(root.get("legacyNumber"), "")), like, '\\'),
                        cb.like(cb.lower(root.get("subject")), like, '\\')));
            }
            if (c.statuses() != null && !c.statuses().isEmpty()) {
                p.add(root.get("status").in(c.statuses()));
            }
            if (Boolean.TRUE.equals(c.openOnly()) && c.openStatuses() != null) {
                p.add(root.get("status").in(c.openStatuses()));
            }
            if (c.priorities() != null && !c.priorities().isEmpty()) {
                p.add(root.get("priority").in(c.priorities()));
            }
            if (c.categoryCodes() != null && !c.categoryCodes().isEmpty()) {
                p.add(root.get("categoryCode").in(c.categoryCodes()));
            }
            String assignee = c.assignee();
            if (assignee != null && !assignee.isBlank()) {
                if ("unassigned".equalsIgnoreCase(assignee)) {
                    p.add(cb.isNull(root.get("assignedAgentId")));
                } else {
                    UUID agent = "me".equalsIgnoreCase(assignee) ? currentUserId : UUID.fromString(assignee);
                    p.add(cb.equal(root.get("assignedAgentId"), agent));
                }
            }
            if (c.requesterId() != null) {
                p.add(cb.equal(root.get("requesterId"), c.requesterId()));
            }
            if (c.requesterRole() != null) {
                p.add(cb.equal(root.get("requesterRole"), c.requesterRole()));
            }
            if (c.escalated() != null) {
                p.add(cb.equal(root.get("escalated"), c.escalated()));
            }
            if (Boolean.TRUE.equals(c.slaBreached())) {
                if (c.openStatuses() != null) {
                    p.add(root.get("status").in(c.openStatuses()));
                }
                p.add(cb.or(
                        cb.and(cb.isNull(root.get("firstRespondedAt")),
                                cb.lessThan(root.get("firstResponseDueAt"), now)),
                        cb.and(cb.isNull(root.get("slaPausedSince")), cb.lessThan(root.get("resolutionDueAt"), now))));
            }
            if (c.createdFrom() != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.createdFrom().atStartOfDay(zone).toInstant()));
            }
            if (c.createdTo() != null) {
                p.add(cb.lessThan(root.get("createdAt"), c.createdTo().plusDays(1).atStartOfDay(zone).toInstant()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
