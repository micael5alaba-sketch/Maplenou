package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.domain.MessageVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TicketMessageJpaRepository extends JpaRepository<TicketMessageEntity, UUID> {

    List<TicketMessageEntity> findByTicketIdAndVisibilityInOrderByCreatedAtAsc(UUID ticketId,
                                                                                Collection<MessageVisibility> visibility);

    Optional<TicketMessageEntity> findByTicketIdAndAuthorIdAndClientMessageId(UUID ticketId, UUID authorId,
                                                                             String clientMessageId);
}

interface TicketAttachmentJpaRepository extends JpaRepository<TicketAttachmentEntity, UUID> {

    @Query("select coalesce(sum(a.sizeBytes), 0) from TicketAttachmentEntity a where a.ticketId = :ticketId")
    long totalBytesForTicket(@Param("ticketId") UUID ticketId);

    List<TicketAttachmentEntity> findByMessageIdInOrderByCreatedAtAsc(Collection<UUID> messageIds);
}
