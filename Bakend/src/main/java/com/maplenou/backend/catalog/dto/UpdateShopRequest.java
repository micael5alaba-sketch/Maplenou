package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.Size;

// Tous les champs sont optionnels : seuls les champs non-null sont appliqués.
public record UpdateShopRequest(
        @Size(max = 150) String name,
        String description,
        @Size(max = 500) String logoUrl,
        @Size(max = 500) String coverUrl,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 50) String taxId
) {
}
