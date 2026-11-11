package com.maplenou.support.ticket.domain.vocabulary;

/**
 * One configurable status.
 *
 * @param slaPaused the resolution clock stops while a ticket has this status (DEC-CS11-03)
 * @param system    one of the six statuses the rules rely on: it cannot be removed and keeps its kind
 */
public record StatusDefinition(String code, StatusKind kind, String labelFr, String labelEn, int sortOrder,
                               boolean slaPaused, boolean system, boolean active) {

    public boolean isOpen() {
        return kind.isOpen();
    }

    public String label(String language) {
        return "en".equalsIgnoreCase(language) && labelEn != null && !labelEn.isBlank() ? labelEn : labelFr;
    }
}
