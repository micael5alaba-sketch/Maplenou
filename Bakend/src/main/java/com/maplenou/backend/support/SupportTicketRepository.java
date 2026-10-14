package com.maplenou.backend.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    @EntityGraph(attributePaths = {"requester", "relatedDelivery"})
    Page<SupportTicket> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "relatedDelivery"})
    Page<SupportTicket> findByStatusOrderByCreatedAtAsc(SupportTicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "relatedDelivery"})
    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Génère "TKT-<année>-<numéro séquentiel sur 6 chiffres>" à partir de la séquence Postgres. */
    @Query(value = "SELECT nextval('support_ticket_seq')", nativeQuery = true)
    long nextSequenceValue();
}
