package com.maplenou.backend.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Vue liste catalogue public : mêmes associations nécessaires
    @Override
    @EntityGraph(attributePaths = {"shop", "category", "images"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    // Vérification d'existence pour ownership check (sans charger les associations)
    Optional<Product> findByIdAndDeletedFalse(UUID id);

    boolean existsBySlug(String slug);
}
