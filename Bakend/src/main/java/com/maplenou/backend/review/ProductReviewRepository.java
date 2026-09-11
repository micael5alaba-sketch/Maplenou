package com.maplenou.backend.review;

import com.maplenou.backend.review.dto.MyReviewRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Note moyenne + nombre d'avis pour plusieurs produits en une seule requête
     * (utilisé pour éviter un appel réseau par carte produit dans les listes de catalogue).
     * Chaque ligne : [0]=productId (UUID), [1]=moyenne (Double), [2]=nombre (Long).
     */
    @Query("SELECT r.product.id, AVG(r.rating), COUNT(r) FROM ProductReview r " +
           "WHERE r.product.id IN :productIds GROUP BY r.product.id")
    List<Object[]> findRatingStatsByProductIds(@Param("productIds") List<UUID> productIds);

    /** Vérifie si l'acheteur a déjà laissé un avis sur ce produit. */
    boolean existsByProductIdAndAuthorId(UUID productId, UUID authorId);

    /** Vérifie que la sous-commande a déjà été utilisée pour un avis sur ce produit. */
    boolean existsByProductIdAndSubOrderId(UUID productId, UUID subOrderId);

    /**
     * Avis écrits par un utilisateur, produits et boutiques confondus (UNION ALL native),
     * du plus récent au plus ancien. Requête native car Spring Data ne sait pas paginer une
     * union de deux entités différentes autrement.
     */
    @Query(
        value = """
                SELECT pr.id AS id, 'PRODUCT' AS type, pr.product_id AS target_id,
                       p.name AS target_name, pr.rating AS rating, pr.comment AS comment,
                       pr.created_at AS created_at
                FROM product_reviews pr JOIN products p ON p.id = pr.product_id
                WHERE pr.user_id = :userId
                UNION ALL
                SELECT sr.id AS id, 'SHOP' AS type, sr.shop_id AS target_id,
                       s.name AS target_name, sr.rating AS rating, sr.comment AS comment,
                       sr.created_at AS created_at
                FROM shop_reviews sr JOIN shops s ON s.id = sr.shop_id
                WHERE sr.user_id = :userId
                ORDER BY created_at DESC
                """,
        countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT id FROM product_reviews WHERE user_id = :userId
                    UNION ALL
                    SELECT id FROM shop_reviews WHERE user_id = :userId
                ) combined
                """,
        nativeQuery = true)
    Page<MyReviewRow> findMyReviews(@Param("userId") UUID userId, Pageable pageable);
}
