package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateVariantRequest(
        @NotBlank @Size(max = 100) String label,
        BigDecimal priceOverride,
        @NotNull @Min(0) Integer stockQuantity,
        @NotBlank @Size(max = 100) String sku
) {
}
