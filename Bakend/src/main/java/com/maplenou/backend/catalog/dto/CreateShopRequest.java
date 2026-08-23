package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShopRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String district
) {
}
