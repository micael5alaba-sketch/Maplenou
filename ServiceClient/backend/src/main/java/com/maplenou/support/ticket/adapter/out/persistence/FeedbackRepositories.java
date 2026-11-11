package com.maplenou.support.ticket.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SatisfactionJpaRepository extends JpaRepository<SatisfactionEntity, UUID> {

    @Query("""
            select count(s), avg(s.rating), sum(case when s.rating >= 4 then 1 else 0 end)
            from SatisfactionEntity s where s.ratedAt >= :from and s.ratedAt < :to
            """)
    List<Object[]> stats(@Param("from") Instant from, @Param("to") Instant to);
}

interface CannedReplyJpaRepository extends JpaRepository<CannedReplyEntity, UUID> {
}

interface AiSuggestionJpaRepository extends JpaRepository<AiSuggestionEntity, UUID> {
}

interface ExternalActionJpaRepository extends JpaRepository<ExternalActionEntity, UUID> {
    List<ExternalActionEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
