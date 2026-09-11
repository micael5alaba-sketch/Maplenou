package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @Size(min = 2, max = 100) String name,
        Boolean active,
        UUID parentId,
        // Optionnel : chaîne vide pour retirer l'image existante.
        @Size(max = 500) String imageUrl
) {
}
