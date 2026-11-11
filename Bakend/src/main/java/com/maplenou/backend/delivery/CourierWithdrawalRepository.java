package com.maplenou.backend.delivery;

import com.maplenou.backend.payout.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CourierWithdrawalRepository extends JpaRepository<CourierWithdrawal, UUID> {

    @Query(value = "SELECT w FROM CourierWithdrawal w JOIN FETCH w.agent WHERE w.agent.id = :agentId ORDER BY w.createdAt DESC",
            countQuery = "SELECT COUNT(w) FROM CourierWithdrawal w WHERE w.agent.id = :agentId")
    Page<CourierWithdrawal> findByAgent(@Param("agentId") UUID agentId, Pageable pageable);

    @Query(value = "SELECT w FROM CourierWithdrawal w JOIN FETCH w.agent WHERE (:status IS NULL OR w.status = :status) ORDER BY w.createdAt DESC",
            countQuery = "SELECT COUNT(w) FROM CourierWithdrawal w WHERE (:status IS NULL OR w.status = :status)")
    Page<CourierWithdrawal> findByOptionalStatus(@Param("status") PayoutStatus status, Pageable pageable);

    @Query("SELECT w FROM CourierWithdrawal w JOIN FETCH w.agent WHERE w.id = :id")
    Optional<CourierWithdrawal> findByIdWithAgent(@Param("id") UUID id);
}
