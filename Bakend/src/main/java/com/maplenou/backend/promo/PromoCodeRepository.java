package com.maplenou.backend.promo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    /** Recherche par code (insensible à la casse). */
    @Query("SELECT p FROM PromoCode p WHERE UPPER(p.code) = UPPER(:code)")
    Optional<PromoCode> findByCodeIgnoreCase(@Param("code") String code);

    /** Codes d'une boutique spécifique. */
    Page<PromoCode> findByScopeTypeAndScopeId(PromoScopeType scopeType, UUID scopeId, Pageable pageable);

    /** Tous les codes plateforme. */
    Page<PromoCode> findByScopeType(PromoScopeType scopeType, Pageable pageable);

    /** Incrémente atomiquement le compteur d'utilisations. */
    @Modifying
    @Query("UPDATE PromoCode p SET p.currentUses = p.currentUses + 1 WHERE p.id = :id")
    void incrementUsage(@Param("id") UUID id);
}
