package com.maplenou.support.ticket.domain.vocabulary;

/**
 * One configurable priority.
 *
 * @param rank                higher = more urgent (sorting, dashboards)
 * @param requesterSelectable requesters may choose it when they create a ticket; the others are given by support
 *                            only (BR-036: « Critique »)
 * @param defaultPriority     used when the requester does not choose
 */
public record PriorityDefinition(String code, int rank, String labelFr, String labelEn, boolean requesterSelectable,
                                 boolean defaultPriority, boolean system, boolean active) {

    public String label(String language) {
        return "en".equalsIgnoreCase(language) && labelEn != null && !labelEn.isBlank() ? labelEn : labelFr;
    }
}
