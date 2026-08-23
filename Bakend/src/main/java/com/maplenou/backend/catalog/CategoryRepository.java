package com.maplenou.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // JOIN FETCH obligatoire : "parent" est LAZY, et ces résultats sont mis en cache Redis
    // (sérialisation JSON hors session Hibernate) via @Cacheable dans CategoryService —
    // sans le fetch, toute catégorie ayant un parent fait échouer la sérialisation (500).
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.slug = :slug")
    Optional<Category> findBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    // Catalogue public : uniquement les catégories actives, racines (sans parent)
    List<Category> findByParentIsNullAndActiveTrueOrderByNameAsc();

    // Sous-catégories actives d'une catégorie donnée
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent WHERE c.parent.id = :parentId AND c.active = true ORDER BY c.name ASC")
    List<Category> findByParentIdAndActiveTrueOrderByNameAsc(@Param("parentId") UUID parentId);
}
