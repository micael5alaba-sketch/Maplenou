package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.domain.ReopenRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketHistoryJpaRepository extends JpaRepository<TicketHistoryEntity, UUID> {

    List<TicketHistoryEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    List<TicketHistoryEntity> findByTicketIdAndRequesterVisibleTrueOrderByCreatedAtAsc(UUID ticketId);
}

interface ReopenRequestJpaRepository extends JpaRepository<ReopenRequestEntity, UUID> {

    Optional<ReopenRequestEntity> findByTicketIdAndStatus(UUID ticketId, ReopenRequest.Status status);

    List<ReopenRequestEntity> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
