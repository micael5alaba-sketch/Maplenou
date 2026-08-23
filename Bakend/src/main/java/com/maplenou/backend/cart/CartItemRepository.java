package com.maplenou.backend.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    // Chargement complet de la chaîne variant → product → shop + images
    // pour construire CartItemResponse sans LazyInitializationException
    @Query("""
            SELECT DISTINCT ci FROM CartItem ci
            JOIN FETCH ci.variant v
            JOIN FETCH v.product p
            JOIN FETCH p.shop s
            LEFT JOIN FETCH p.images img
            WHERE ci.cart.id = :cartId
            ORDER BY ci.createdAt ASC
            """)
    List<CartItem> findByCartIdWithDetails(@Param("cartId") UUID cartId);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);

    Optional<CartItem> findByCartIdAndVariantId(UUID cartId, UUID variantId);
}
