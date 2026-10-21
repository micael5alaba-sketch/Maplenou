package com.maplenou.backend.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.maplenou.backend.user.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findBySubOrderId(UUID subOrderId);

    /** Livraisons assignées à un livreur. */
    @EntityGraph(attributePaths = {"subOrder", "subOrder.shop", "subOrder.order", "agent"})
    Page<Delivery> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    /** Livraisons en attente d'affectation (admin). */
    @EntityGraph(attributePaths = {"subOrder", "subOrder.shop", "subOrder.order"})
    Page<Delivery> findByStatusOrderByCreatedAtAsc(DeliveryStatus status, Pageable pageable);

    @Query("""
            SELECT d FROM Delivery d
            LEFT JOIN FETCH d.subOrder so
            LEFT JOIN FETCH so.shop
            LEFT JOIN FETCH so.order o
            LEFT JOIN FETCH o.buyer
            LEFT JOIN FETCH d.agent
            WHERE d.id = :id
            """)
    Optional<Delivery> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT d FROM Delivery d
            LEFT JOIN FETCH d.subOrder so
            LEFT JOIN FETCH so.shop
            LEFT JOIN FETCH so.order o
            LEFT JOIN FETCH o.buyer
            LEFT JOIN FETCH d.agent
            WHERE so.id = :subOrderId
            """)
    Optional<Delivery> findBySubOrderIdWithDetails(@Param("subOrderId") UUID subOrderId);

    /**
     * Prise en charge en libre-service par le livreur lui-même (dispatch signalé manquant par
     * l'audit externe du 16/10/2026 — jusqu'ici seul un admin pouvait affecter un livreur).
     * Conditionnée sur {@code status = PENDING} dans la clause WHERE : mise à jour atomique,
     * exactement comme la décrémentation de stock (ProductVariantRepository.decrementStock) —
     * si deux livreurs tentent la même course en même temps, un seul UPDATE affecte une ligne,
     * l'autre reçoit 0 (voir DeliveryService.claim). Pas de lecture-puis-écriture séparée qui
     * laisserait une fenêtre de course critique.
     */
    @Modifying
    @Query("UPDATE Delivery d SET d.agent = :agent, d.status = com.maplenou.backend.delivery.DeliveryStatus.ASSIGNED, " +
           "d.assignedAt = :now WHERE d.id = :id AND d.status = com.maplenou.backend.delivery.DeliveryStatus.PENDING")
    int claim(@Param("id") UUID id, @Param("agent") User agent, @Param("now") Instant now);

    // ----- KPI queries -----

    long countByStatus(DeliveryStatus status);
}
