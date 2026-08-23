package com.maplenou.backend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {

    /** Liste des sous-ordres d'une boutique — pour le vendeur. */
    @EntityGraph(attributePaths = {"shop", "order"})
    Page<SubOrder> findByShopIdOrderByCreatedAtDesc(UUID shopId, Pageable pageable);

    /** Détail complet d'un sous-ordre avec ses articles. */
    @Query("""
            SELECT so FROM SubOrder so
            LEFT JOIN FETCH so.shop
            LEFT JOIN FETCH so.order o
            LEFT JOIN FETCH o.buyer
            LEFT JOIN FETCH so.items
            WHERE so.id = :id
            """)
    Optional<SubOrder> findByIdWithDetails(@Param("id") UUID id);

    /** Recherche d'un sous-ordre appartenant à une boutique précise (vérification ownership). */
    Optional<SubOrder> findByIdAndShopId(UUID id, UUID shopId);

    // ----- KPI queries -----

    @Query("SELECT COALESCE(SUM(so.commissionAmount), 0) FROM SubOrder so WHERE so.status <> com.maplenou.backend.order.SubOrderStatus.CANCELLED")
    BigDecimal sumTotalCommission();

    // ----- Payout queries -----

    /**
     * IDs distincts des boutiques ayant des sous-ordres DELIVERED non encore payés
     * dont le statut a été mis à jour dans la période donnée.
     */
    @Query("""
            SELECT DISTINCT so.shop.id FROM SubOrder so
            WHERE so.status = com.maplenou.backend.order.SubOrderStatus.DELIVERED
            AND so.payout IS NULL
            AND so.updatedAt >= :from
            AND so.updatedAt <= :to
            """)
    List<UUID> findDistinctShopIdsWithUnpaidDelivered(
            @Param("from") Instant from,
            @Param("to") Instant to);

    /**
     * Sous-ordres DELIVERED non encore payés pour une boutique dans une période.
     * Utilisé par le scheduler de génération de payouts.
     */
    @Query("""
            SELECT so FROM SubOrder so
            JOIN FETCH so.shop
            WHERE so.status = com.maplenou.backend.order.SubOrderStatus.DELIVERED
            AND so.payout IS NULL
            AND so.shop.id = :shopId
            AND so.updatedAt >= :from
            AND so.updatedAt <= :to
            """)
    List<SubOrder> findDeliveredUnpaidByShopIdAndPeriod(
            @Param("shopId") UUID shopId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
