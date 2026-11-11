package com.maplenou.support.ticket.domain;

import java.util.UUID;

/**
 * Ticket category (BR-006). Administrators may add, rename, reorder and deactivate categories; a category is never
 * deleted (tickets keep it) and « Autre » ({@code OTHER}) always stays active (DEC-CS11-06).
 */
public record Category(UUID id, String code, String name, String nameEn, String description, String descriptionEn,
                       int sortOrder, boolean active) {

    public static final String OTHER = "OTHER";

    public String label(String language) {
        return "en".equalsIgnoreCase(language) && nameEn != null && !nameEn.isBlank() ? nameEn : name;
    }
}
