package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

// Tous les champs sont optionnels : seuls les non-null sont appliqués.
public record UpdateProductRequest(
        @Size(max = 200) String name,
        String description,
        @DecimalMin("500.00") BigDecimal basePrice,
        UUID categoryId,
        ProductStatus status
) {
}
