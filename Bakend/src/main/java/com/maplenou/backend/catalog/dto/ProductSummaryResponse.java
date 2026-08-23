package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

// Vue allégée pour les listes (catalogue public, liste vendeur).
public record ProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        BigDecimal basePrice,
        ProductStatus status,
        UUID shopId,
        String shopName,
        UUID categoryId,
        String thumbnailUrl   // première image ou null
) {
    public static ProductSummaryResponse from(Product p) {
        String thumbnail = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();
        return new ProductSummaryResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getBasePrice(),
                p.getStatus(),
                p.getShop().getId(),
                p.getShop().getName(),
                p.getCategory().getId(),
                thumbnail
        );
    }
}
