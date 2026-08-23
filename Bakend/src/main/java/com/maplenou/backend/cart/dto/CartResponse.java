package com.maplenou.backend.cart.dto;

import com.maplenou.backend.cart.Cart;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID cartId,
        List<CartItemResponse> items,
        // Total uniquement sur les articles disponibles (produits encore actifs)
        BigDecimal totalAmount,
        int itemCount
) {
    public static CartResponse from(Cart cart, List<CartItemResponse> items) {
        BigDecimal total = items.stream()
                .filter(CartItemResponse::available)
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, total, items.size());
    }

    // Panier vide (aucun enregistrement en base encore)
    public static CartResponse empty() {
        return new CartResponse(null, List.of(), BigDecimal.ZERO, 0);
    }
}
