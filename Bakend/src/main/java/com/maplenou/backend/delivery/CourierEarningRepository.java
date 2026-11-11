package com.maplenou.backend.delivery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface CourierEarningRepository extends JpaRepository<CourierEarning, UUID> {

    Page<CourierEarning> findByAgentIdOrderByCreatedAtDesc(UUID agentId, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(e.amount), 0) FROM CourierEarning e
            WHERE e.agent.id = :agentId AND e.status = com.maplenou.backend.delivery.CourierEarningStatus.PENDING
              AND e.withdrawal IS NULL
            """)
    BigDecimal sumAvailableBalanceByAgentId(@Param("agentId") UUID agentId);

    /** Gains versables : PENDING et pas encore inclus dans un retrait. */
    @Query("""
            SELECT e FROM CourierEarning e
            WHERE e.agent.id = :agentId AND e.status = com.maplenou.backend.delivery.CourierEarningStatus.PENDING
              AND e.withdrawal IS NULL
            """)
    java.util.List<CourierEarning> findAvailableForWithdrawal(@Param("agentId") UUID agentId);

    @Query("SELECT e FROM CourierEarning e WHERE e.withdrawal.id = :withdrawalId")
    java.util.List<CourierEarning> findByWithdrawalId(@Param("withdrawalId") UUID withdrawalId);

    // ----- Admin -----

    /** Tous les gains livreurs, filtrables par statut (vue admin). */
    @Query("SELECT e FROM CourierEarning e WHERE (:status IS NULL OR e.status = :status) ORDER BY e.createdAt DESC")
    Page<CourierEarning> findByOptionalStatus(@Param("status") CourierEarningStatus status, Pageable pageable);

    /** Gain avec son livreur chargé (pour marquer PAID et journaliser). */
    @Query("""
            SELECT e FROM CourierEarning e
            JOIN FETCH e.agent
            WHERE e.id = :id
            """)
    Optional<CourierEarning> findByIdWithAgent(@Param("id") UUID id);
}
