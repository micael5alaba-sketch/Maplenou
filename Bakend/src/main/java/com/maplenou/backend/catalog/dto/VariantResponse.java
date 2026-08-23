package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String label,
        BigDecimal priceOverride,
        int stockQuantity,
        String sku
) {
    public static VariantResponse from(ProductVariant v) {
        return new VariantResponse(
                v.getId(),
                v.getLabel(),
                v.getPriceOverride(),
                v.getStockQuantity(),
                v.getSku()
        );
    }
}
