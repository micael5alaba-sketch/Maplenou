package com.maplenou.backend.cart;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId);

    // product + shop chargés via EntityGraph (associations simples, pas de collection → pas de pagination en mémoire)
    // product.images se charge en batch lazy grâce à hibernate.default_batch_fetch_size
    @EntityGraph(attributePaths = {"product", "product.shop"})
    Page<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
