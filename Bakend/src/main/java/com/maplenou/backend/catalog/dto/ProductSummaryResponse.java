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
        BigDecimal oldPrice,   // null = pas de promo affichée
        ProductStatus status,
        UUID shopId,
        String shopName,
        UUID categoryId,
        String thumbnailUrl,   // première image ou null
        double averageRating,  // 0 si aucun avis
        long reviewCount
) {
    // Sans statistiques d'avis (ex: appelant qui ne les a pas chargées en masse).
    public static ProductSummaryResponse from(Product p) {
        return from(p, 0.0, 0L);
    }

    public static ProductSummaryResponse from(Product p, double averageRating, long reviewCount) {
        String thumbnail = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();
        return new ProductSummaryResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getBasePrice(),
                p.getOldPrice(),
                p.getStatus(),
                p.getShop().getId(),
                p.getShop().getName(),
                p.getCategory().getId(),
                thumbnail,
                averageRating,
                reviewCount
        );
    }
}
