package com.maplenou.backend.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;



public interface ShopRepository extends JpaRepository<Shop, UUID> {

    // owner chargé en même temps → ShopResponse.from() peut accéder owner.getId() sans session active
    @EntityGraph(attributePaths = {"owner"})
    Optional<Shop> findBySlug(String slug);

    @EntityGraph(attributePaths = {"owner"})
    Optional<Shop> findByOwnerId(UUID ownerId);

    boolean existsBySlug(String slug);

    boolean existsByOwnerId(UUID ownerId);

    @EntityGraph(attributePaths = {"owner"})
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    Page<Shop> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"owner"})
    Optional<Shop> findById(UUID id);

    // ----- KPI queries -----

    long countByStatus(ShopStatus status);
}
