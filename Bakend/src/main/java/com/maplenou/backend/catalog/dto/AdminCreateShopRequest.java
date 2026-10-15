package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AdminCreateShopRequest(
        @NotNull UUID ownerId,
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String district,
        @Size(max = 50) String taxId
) {
}
