package com.maplenou.backend.cart.dto;

import com.maplenou.backend.cart.CartItem;
import com.maplenou.backend.catalog.Product;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID cartItemId,
        UUID productVariantId,
        UUID productId,
        String productName,
        String productSlug,
        String variantLabel,
        String sku,
        BigDecimal effectivePrice,
        int quantity,
        BigDecimal subtotal,
        UUID shopId,
        String shopName,
        String thumbnailUrl,
        boolean available   // false si le produit est archivé ou supprimé
) {
    public static CartItemResponse from(CartItem item) {
        ProductVariant variant = item.getVariant();
        Product product = variant.getProduct();

        // Le prix effectif est la surcharge de variante, sinon le prix de base du produit.
        BigDecimal effectivePrice = variant.getPriceOverride() != null
                ? variant.getPriceOverride()
                : product.getBasePrice();

        String thumbnail = product.getImages().isEmpty()
                ? null
                : product.getImages().get(0).getUrl();

        boolean available = product.getStatus() == ProductStatus.ACTIVE && !product.isDeleted();

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                variant.getLabel(),
                variant.getSku(),
                effectivePrice,
                item.getQuantity(),
                effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                product.getShop().getId(),
                product.getShop().getName(),
                thumbnail,
                available
        );
    }
}
