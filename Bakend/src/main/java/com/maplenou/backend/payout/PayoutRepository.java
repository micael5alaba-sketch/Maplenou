package com.maplenou.backend.payout;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    /** Payouts d'une boutique (vue vendeur). */
    Page<Payout> findByShopIdOrderByCreatedAtDesc(UUID shopId, Pageable pageable);

    /** Tous les payouts filtrables par statut (vue admin). */
    @Query("SELECT p FROM Payout p WHERE (:status IS NULL OR p.status = :status) ORDER BY p.createdAt DESC")
    Page<Payout> findByOptionalStatus(@Param("status") PayoutStatus status, Pageable pageable);

    /** Vérifie qu'un payout n'existe pas déjà pour cette boutique sur cette période. */
    boolean existsByShopIdAndPeriodStartAndPeriodEnd(UUID shopId, Instant periodStart, Instant periodEnd);

    /** Payout avec sa boutique chargée (pour le détail admin). */
    @Query("""
            SELECT p FROM Payout p
            JOIN FETCH p.shop
            WHERE p.id = :id
            """)
    Optional<Payout> findByIdWithShop(@Param("id") UUID id);
}
