package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.Category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        UUID parentId,
        String imageUrl,   // null = le frontend garde son icône générique
        boolean active
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getImageUrl(),
                category.isActive()
        );
    }
}
