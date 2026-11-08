package com.maplenou.support.ticket.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketJpaRepository extends JpaRepository<TicketEntity, UUID>, JpaSpecificationExecutor<TicketEntity> {

    @Query(value = "select nextval('ticket_number_seq')", nativeQuery = true)
    long nextTicketNumber();

    boolean existsByLegacyId(UUID legacyId);

    /** Row lock used by every command so concurrent changes to one ticket are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketEntity t where t.id = :id")
    Optional<TicketEntity> findForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("update TicketEntity t set t.priorityRank = :rank where t.priority = :code")
    int updatePriorityRank(@Param("code") String code, @Param("rank") int rank);

    long countByRequesterId(UUID requesterId);

    long countByStatusIn(Collection<String> statuses);

    long countByAssignedAgentIdIsNullAndStatusIn(Collection<String> statuses);

    long countByAssignedAgentIdAndStatusIn(UUID agentId, Collection<String> statuses);

    long countByEscalatedTrueAndStatusIn(Collection<String> statuses);

    long countByResolvedAtGreaterThanEqual(Instant since);

    boolean existsByStatus(String status);

    boolean existsByPriority(String priority);

    @Query("""
            select count(t) from TicketEntity t where t.status in :open and (
              (t.firstRespondedAt is null and t.firstResponseDueAt < :now)
              or (t.slaPausedSince is null and t.resolutionDueAt < :now))
            """)
    long countBreached(@Param("open") Collection<String> open, @Param("now") Instant now);

    @Query("select t.priority, count(t) from TicketEntity t where t.status in :statuses group by t.priority")
    List<Object[]> countByPriority(@Param("statuses") Collection<String> statuses);

    @Query("select t.categoryCode, count(t) from TicketEntity t where t.status in :statuses group by t.categoryCode")
    List<Object[]> countByCategory(@Param("statuses") Collection<String> statuses);

    @Query("""
            select t.assignedAgentId, count(t) from TicketEntity t
            where t.assignedAgentId is not null and t.status in :statuses group by t.assignedAgentId
            """)
    List<Object[]> openLoadByAgent(@Param("statuses") Collection<String> statuses);

    @Query("select t from TicketEntity t where t.createdAt >= :from and t.createdAt < :to")
    List<TicketEntity> findCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select t.id from TicketEntity t
            where t.status in :resolved and t.closedAt is null and t.resolvedAt < :before order by t.resolvedAt
            """)
    List<UUID> findResolvedBefore(@Param("resolved") Collection<String> resolved, @Param("before") Instant before,
                                  org.springframework.data.domain.Pageable page);
}
