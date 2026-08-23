package com.maplenou.backend.cart.dto;

import com.maplenou.backend.cart.Favorite;
import com.maplenou.backend.catalog.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FavoriteResponse(
        UUID favoriteId,
        UUID productId,
        String productName,
        String productSlug,
        BigDecimal basePrice,
        String thumbnailUrl,
        UUID shopId,
        String shopName,
        Instant addedAt
) {
    public static FavoriteResponse from(Favorite favorite) {
        Product p = favorite.getProduct();
        // product.images se charge en batch lazy si accédé dans une transaction active
        String thumbnail = p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl();

        return new FavoriteResponse(
                favorite.getId(),
                p.getId(),
                p.getName(),
                p.getSlug(),
                p.getBasePrice(),
                thumbnail,
                p.getShop().getId(),
                p.getShop().getName(),
                favorite.getCreatedAt()
        );
    }
}
