package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductSpecification;
import com.maplenou.backend.catalog.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Vue complète pour la page détail produit.
public record ProductDetailResponse(
        UUID id,
        String name,
        String slug,
        String description,
        BigDecimal basePrice,
        BigDecimal oldPrice,   // null = pas de promo affichée
        ProductStatus status,
        UUID shopId,
        String shopName,
        UUID categoryId,
        String categoryName,
        List<VariantResponse> variants,
        List<ImageResponse> images,
        List<ProductSpecification> specifications,
        Instant createdAt
) {
    public static ProductDetailResponse from(Product p) {
        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getDescription(),
                p.getBasePrice(),
                p.getOldPrice(),
                p.getStatus(),
                p.getShop().getId(),
                p.getShop().getName(),
                p.getCategory().getId(),
                p.getCategory().getName(),
                p.getVariants().stream().map(VariantResponse::from).toList(),
                p.getImages().stream().map(ImageResponse::from).toList(),
                p.getSpecifications() != null ? p.getSpecifications() : List.of(),
                p.getCreatedAt()
        );
    }
}
