package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Vue liste pour le vendeur (`GET /api/shops/mine/products`) — comme
 * {@link ProductSummaryResponse} mais avec [totalStock] en plus (somme du
 * stock de toutes les variantes). Volumé borné (catalogue d'un seul
 * vendeur, jamais des dizaines de milliers de lignes), donc pas de risque
 * de performance à charger les variantes ici — contrairement au catalogue
 * public, qui garde `ProductSummaryResponse` tel quel.
 */
public record SellerProductSummaryResponse(
        UUID id,
        String name,
        String slug,
        BigDecimal basePrice,
        BigDecimal oldPrice,
        ProductStatus status,
        String thumbnailUrl,
        double averageRating,
        long reviewCount,
        int totalStock
) {
    public static SellerProductSummaryResponse from(Product p, double averageRating, long reviewCount) {
        String thumbnail = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();
        int totalStock = p.getVariants().stream().mapToInt(ProductVariant::getStockQuantity).sum();
        return new SellerProductSummaryResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getBasePrice(),
                p.getOldPrice(),
                p.getStatus(),
                thumbnail,
                averageRating,
                reviewCount,
                totalStock
        );
    }
}
