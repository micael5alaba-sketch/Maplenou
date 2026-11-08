package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.TicketMessage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_messages")
class TicketMessageEntity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;
    @Column(name = "author_id", nullable = false, updatable = false)
    private UUID authorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, updatable = false)
    private Role authorRole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private MessageVisibility visibility;
    @Column(nullable = false, updatable = false)
    private String body;
    @Column(name = "client_message_id", updatable = false)
    private String clientMessageId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketMessageEntity() {
    }

    TicketMessageEntity(TicketMessage m) {
        super(m.id());
        this.ticketId = m.ticketId();
        this.authorId = m.authorId();
        this.authorRole = m.authorRole();
        this.visibility = m.visibility();
        this.body = m.body();
        this.clientMessageId = m.clientMessageId();
        this.createdAt = m.createdAt();
    }

    TicketMessage toDomain() {
        return new TicketMessage(getId(), ticketId, authorId, authorRole, visibility, body, clientMessageId, createdAt);
    }
}
