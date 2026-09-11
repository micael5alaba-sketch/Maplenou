package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateVariantRequest(
        @Size(max = 100) String label,
        BigDecimal priceOverride,
        @Min(0) Integer stockQuantity,
        // Permet au vendeur de renseigner/corriger le SKU apres coup (ex: sa propre nomenclature).
        @Size(max = 100) String sku
) {
}
