package com.maplenou.backend.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {

    /** Avis publics d'un produit, du plus récent au plus ancien. */
    @Query(
        value = """
                SELECT r FROM ProductReview r
                LEFT JOIN FETCH r.author
                WHERE r.product.id = :productId
                ORDER BY r.createdAt DESC
                """,
        countQuery = "SELECT count(r) FROM ProductReview r WHERE r.product.id = :productId"
    )
    Page<ProductReview> findByProductIdWithAuthor(@Param("productId") UUID productId, Pageable pageable);

    /** Note moyenne d'un produit. */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Optional<Double> findAverageRatingByProductId(@Param("productId") UUID productId);

    /** Nombre d'avis d'un produit. */
    long countByProductId(UUID productId);

    /** Vérifie si l'acheteur a déjà laissé un avis sur ce produit. */
    boolean existsByProductIdAndAuthorId(UUID productId, UUID authorId);

    /** Vérifie que la sous-commande a déjà été utilisée pour un avis sur ce produit. */
    boolean existsByProductIdAndSubOrderId(UUID productId, UUID subOrderId);
}
