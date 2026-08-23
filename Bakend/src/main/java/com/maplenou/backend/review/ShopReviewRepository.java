package com.maplenou.backend.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShopReviewRepository extends JpaRepository<ShopReview, UUID> {

    /** Avis publics d'une boutique, du plus récent au plus ancien. */
    @Query(
        value = """
                SELECT r FROM ShopReview r
                LEFT JOIN FETCH r.author
                WHERE r.shop.id = :shopId
                ORDER BY r.createdAt DESC
                """,
        countQuery = "SELECT count(r) FROM ShopReview r WHERE r.shop.id = :shopId"
    )
    Page<ShopReview> findByShopIdWithAuthor(@Param("shopId") UUID shopId, Pageable pageable);

    /** Note moyenne d'une boutique. */
    @Query("SELECT AVG(r.rating) FROM ShopReview r WHERE r.shop.id = :shopId")
    Optional<Double> findAverageRatingByShopId(@Param("shopId") UUID shopId);

    /** Nombre d'avis d'une boutique. */
    long countByShopId(UUID shopId);

    /** Vérifie si l'acheteur a déjà laissé un avis sur cette boutique. */
    boolean existsByShopIdAndAuthorId(UUID shopId, UUID authorId);

    /** Vérifie que la sous-commande a déjà été utilisée pour un avis sur cette boutique. */
    boolean existsByShopIdAndSubOrderId(UUID shopId, UUID subOrderId);
}
