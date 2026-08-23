package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank @Size(min = 2, max = 100) String name,

        // slug optionnel : si absent, généré automatiquement à partir du nom
        @Size(max = 120) String slug,

        UUID parentId
) {
}
