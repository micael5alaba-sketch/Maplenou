package com.maplenou.backend.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // ----- KPI queries -----

    long countByStatus(DeliveryStatus status);
}
