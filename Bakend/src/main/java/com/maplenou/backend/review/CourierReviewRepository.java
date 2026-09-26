package com.maplenou.backend.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourierReviewRepository extends JpaRepository<CourierReview, UUID> {

    /** Une seule note par livraison. */
    boolean existsByDeliveryId(UUID deliveryId);

    /** Avis publics d'un livreur, du plus récent au plus ancien. */
    @Query(
        value = """
                SELECT r FROM CourierReview r
                LEFT JOIN FETCH r.author
                WHERE r.agent.id = :agentId
                ORDER BY r.createdAt DESC
                """,
        countQuery = "SELECT count(r) FROM CourierReview r WHERE r.agent.id = :agentId"
    )
    Page<CourierReview> findByAgentIdWithAuthor(@Param("agentId") UUID agentId, Pageable pageable);

    /** Note moyenne d'un livreur. */
    @Query("SELECT AVG(r.rating) FROM CourierReview r WHERE r.agent.id = :agentId")
    Optional<Double> findAverageRatingByAgentId(@Param("agentId") UUID agentId);

    /** Nombre d'avis d'un livreur. */
    long countByAgentId(UUID agentId);
}
