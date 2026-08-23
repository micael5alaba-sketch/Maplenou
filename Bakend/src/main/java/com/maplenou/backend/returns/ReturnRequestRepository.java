package com.maplenou.backend.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    /**
     * Vue acheteur : mes demandes de retour.
     * countQuery séparé obligatoire : JOIN FETCH incompatible avec la dérivation auto de COUNT.
     */
    @Query(
        value = """
                SELECT r FROM ReturnRequest r
                LEFT JOIN FETCH r.subOrder so
                LEFT JOIN FETCH so.shop
                WHERE r.buyer.id = :buyerId
                ORDER BY r.createdAt DESC
                """,
        countQuery = """
                SELECT count(r) FROM ReturnRequest r
                WHERE r.buyer.id = :buyerId
                """
    )
    Page<ReturnRequest> findByBuyerIdWithDetails(@Param("buyerId") UUID buyerId, Pageable pageable);

    /**
     * Vue vendeur : retours sur les sous-commandes de ma boutique.
     */
    @Query(
        value = """
                SELECT r FROM ReturnRequest r
                LEFT JOIN FETCH r.subOrder so
                LEFT JOIN FETCH so.shop
                LEFT JOIN FETCH r.buyer
                WHERE so.shop.id = :shopId
                ORDER BY r.createdAt DESC
                """,
        countQuery = """
                SELECT count(r) FROM ReturnRequest r
                WHERE r.subOrder.shop.id = :shopId
                """
    )
    Page<ReturnRequest> findByShopIdWithDetails(@Param("shopId") UUID shopId, Pageable pageable);

    /**
     * Admin : retours en attente de décision.
     */
    @Query(
        value = """
                SELECT r FROM ReturnRequest r
                LEFT JOIN FETCH r.subOrder so
                LEFT JOIN FETCH so.shop
                LEFT JOIN FETCH r.buyer
                WHERE r.decision IS NULL
                ORDER BY r.createdAt ASC
                """,
        countQuery = """
                SELECT count(r) FROM ReturnRequest r
                WHERE r.decision IS NULL
                """
    )
    Page<ReturnRequest> findPendingDecision(Pageable pageable);

    /**
     * Détail complet — pour un seul enregistrement, pas de problème de pagination.
     */
    @Query("""
            SELECT r FROM ReturnRequest r
            LEFT JOIN FETCH r.subOrder so
            LEFT JOIN FETCH so.shop
            LEFT JOIN FETCH so.order o
            LEFT JOIN FETCH o.buyer
            LEFT JOIN FETCH so.items
            LEFT JOIN FETCH r.buyer
            LEFT JOIN FETCH r.decidedBy
            WHERE r.id = :id
            """)
    Optional<ReturnRequest> findByIdWithDetails(@Param("id") UUID id);

    boolean existsBySubOrderId(UUID subOrderId);

    Optional<ReturnRequest> findBySubOrderId(UUID subOrderId);
}
