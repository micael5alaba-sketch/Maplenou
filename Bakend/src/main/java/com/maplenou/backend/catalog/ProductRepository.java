package com.maplenou.backend.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    // Vue détail : shop/category/variants en JOIN FETCH ; "images" reste lazy et se charge via
    // hibernate.default_batch_fetch_size (une requête IN groupée), déjà configuré dans le projet.
    // NE PAS ajouter "LEFT JOIN FETCH p.images" ici : Hibernate refuse de fetch-jointer deux
    // collections List ("bags") en même temps (variants + images) — org.hibernate.loader.
    // MultipleBagFetchException. Les appelants tournent tous dans une transaction ouverte
    // (@Transactional), donc l'accès lazy à .getImages() fonctionne normalement ensuite.
    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.shop
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.variants
            WHERE p.id = :id AND p.deleted = false
            """)
    Optional<Product> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.shop
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.variants
            WHERE p.slug = :slug AND p.deleted = false
            """)
    Optional<Product> findBySlugWithDetails(@Param("slug") String slug);

    // Vue liste vendeur : shop + category + images pour ProductSummaryResponse
    @EntityGraph(attributePaths = {"shop", "category", "images"})
    Page<Product> findByShopIdAndDeletedFalse(UUID shopId, Pageable pageable);

    // Idem, avec recherche par nom — catalogue borné à une seule boutique (pas besoin de
    // full-text tsvector ici, contrairement au catalogue public, cf. CLAUDE.md §9).
    @EntityGraph(attributePaths = {"shop", "category", "images"})
    Page<Product> findByShopIdAndDeletedFalseAndNameContainingIgnoreCase(UUID shopId, String name, Pageable pageable);

    // Vue liste catalogue public : mêmes associations nécessaires
    @Override
    @EntityGraph(attributePaths = {"shop", "category", "images"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    // Vérification d'existence pour ownership check (sans charger les associations)
    Optional<Product> findByIdAndDeletedFalse(UUID id);

    boolean existsBySlug(String slug);

    // Recherche catalogue public : full-text natif sur search_vector (colonne generee, index
    // GIN — voir V46). Requete native car le Criteria API n'exprime pas l'operateur `@@` ; le
    // planner Postgres exploite l'index GIN directement sur ce predicat (contrairement a une
    // fonction wrapper qui l'empecherait). Filtres identiques a buildCatalogSpec, meme pagination
    // par curseur (created_at, id) descendant.
    @Query(value = """
            SELECT p.id FROM products p
            WHERE p.search_vector @@ plainto_tsquery('french', :q)
              AND p.status = 'ACTIVE' AND p.is_deleted = false
              AND (CAST(:categoryId AS uuid) IS NULL OR p.category_id = :categoryId)
              AND (CAST(:shopId AS uuid) IS NULL OR p.shop_id = :shopId)
              AND (CAST(:minPrice AS numeric) IS NULL OR p.base_price >= :minPrice)
              AND (CAST(:maxPrice AS numeric) IS NULL OR p.base_price <= :maxPrice)
              AND (
                    CAST(:cursorCreatedAt AS timestamptz) IS NULL
                    OR p.created_at < :cursorCreatedAt
                    OR (p.created_at = :cursorCreatedAt AND p.id < CAST(:cursorId AS uuid))
                  )
            ORDER BY p.created_at DESC, p.id DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> searchCatalogIds(@Param("q") String q,
                                 @Param("categoryId") UUID categoryId,
                                 @Param("shopId") UUID shopId,
                                 @Param("minPrice") BigDecimal minPrice,
                                 @Param("maxPrice") BigDecimal maxPrice,
                                 @Param("cursorCreatedAt") Instant cursorCreatedAt,
                                 @Param("cursorId") UUID cursorId,
                                 @Param("limit") int limit);

    @EntityGraph(attributePaths = {"shop", "category", "images"})
    List<Product> findByIdIn(List<UUID> ids);
}
