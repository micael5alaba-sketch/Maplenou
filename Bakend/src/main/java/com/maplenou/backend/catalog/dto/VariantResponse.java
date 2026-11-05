package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ProductVariant;
import com.maplenou.backend.order.CommissionService;

import java.math.BigDecimal;
import java.util.UUID;

public record VariantResponse(
        UUID id,
        String label,
        BigDecimal priceOverride,
        int stockQuantity,
        String sku,
        BigDecimal displayPrice   // prix effectif paye par l'acheteur (commission incluse)
) {
    public static VariantResponse from(ProductVariant v) {
        return new VariantResponse(
                v.getId(),
                v.getLabel(),
                v.getPriceOverride(),
                v.getStockQuantity(),
                v.getSku(),
                CommissionService.displayPriceFor(v.getProduct(), CommissionService.effectiveBasePrice(v))
        );
    }
}
