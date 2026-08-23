package com.maplenou.backend.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull @DecimalMin("500.00") BigDecimal basePrice,
        @NotNull UUID categoryId,
        // Au moins une variante est requise pour pouvoir vendre
        @NotNull @Size(min = 1) @Valid List<CreateVariantRequest> variants
) {
}
