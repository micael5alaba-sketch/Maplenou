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

    /** Idem, filtré par statut (onglets "À préparer", "En livraison"... côté vendeur). */
    @EntityGraph(attributePaths = {"shop", "order"})
    Page<SubOrder> findByShopIdAndStatusOrderByCreatedAtDesc(UUID shopId, SubOrderStatus status, Pageable pageable);

    /** Sous-commandes incluses dans un reversement donné — pour la facture détaillée vendeur. */
    @EntityGraph(attributePaths = {"shop", "order"})
    List<SubOrder> findByPayoutId(UUID payoutId);

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

    /** Admin : toutes les sous-commandes, toutes boutiques confondues — rien d'équivalent
     * n'existait, l'admin n'avait aucun moyen de voir les commandes passées sur la plateforme. */
    @EntityGraph(attributePaths = {"shop", "order"})
    Page<SubOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"shop", "order"})
    Page<SubOrder> findByStatusOrderByCreatedAtDesc(SubOrderStatus status, Pageable pageable);

    // ----- KPI queries -----

    @Query("SELECT COALESCE(SUM(so.commissionAmount), 0) FROM SubOrder so WHERE so.status <> com.maplenou.backend.order.SubOrderStatus.CANCELLED")
    BigDecimal sumTotalCommission();

    // ----- KPI vendeur (dashboard) -----

    /** Sous-commandes d'une boutique créées après une date donnée — pour le graphique de ventes. */
    @Query("SELECT so FROM SubOrder so WHERE so.shop.id = :shopId AND so.createdAt >= :since ORDER BY so.createdAt ASC")
    List<SubOrder> findByShopIdSince(@Param("shopId") UUID shopId, @Param("since") Instant since);

    /** Nombre de sous-commandes en cours (ni livrées, ni annulées/retournées) d'une boutique. */
    @Query("""
            SELECT COUNT(so) FROM SubOrder so
            WHERE so.shop.id = :shopId
            AND so.status NOT IN (
                com.maplenou.backend.order.SubOrderStatus.DELIVERED,
                com.maplenou.backend.order.SubOrderStatus.CANCELLED,
                com.maplenou.backend.order.SubOrderStatus.RETURNED
            )
            """)
    long countOngoingByShopId(@Param("shopId") UUID shopId);

    long countByShopIdAndStatus(UUID shopId, SubOrderStatus status);

    /**
     * Ventes éligibles (DELIVERED) d'une boutique dont la livraison est tombée dans une période
     * donnée — utilisé pour vérifier si un mois de facturation a encore au moins une vente
     * éligible après le retour de l'une d'elles (voir {@code SellerFeeService}).
     */
    @Query("""
            SELECT COUNT(so) FROM SubOrder so
            JOIN Delivery d ON d.subOrder = so
            WHERE so.shop.id = :shopId
            AND so.status = com.maplenou.backend.order.SubOrderStatus.DELIVERED
            AND d.deliveredAt >= :from
            AND d.deliveredAt < :to
            """)
    long countEligibleByShopIdAndDeliveredAtBetween(
            @Param("shopId") UUID shopId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** Solde disponible : livré mais pas encore rattaché à un `Payout` (voir `PayoutService`). */
    @Query("""
            SELECT COALESCE(SUM(so.netAmount), 0) FROM SubOrder so
            WHERE so.shop.id = :shopId
            AND so.status = com.maplenou.backend.order.SubOrderStatus.DELIVERED
            AND so.payout IS NULL
            """)
    BigDecimal sumAvailableBalanceByShopId(@Param("shopId") UUID shopId);

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
