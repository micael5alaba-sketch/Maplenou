package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.domain.HistoryAction;
import com.maplenou.support.ticket.domain.HistoryEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** Append-only (BR-017, BR-018): the database refuses updates and deletes. */
@Entity
@Immutable
@Table(name = "ticket_history")
class TicketHistoryEntity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;
    @Column(name = "actor_id")
    private UUID actorId;
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role")
    private Role actorRole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HistoryAction action;
    @Column(name = "old_value")
    private String oldValue;
    @Column(name = "new_value")
    private String newValue;
    private String details;
    @Column(name = "requester_visible", nullable = false)
    private boolean requesterVisible;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketHistoryEntity() {
    }

    TicketHistoryEntity(HistoryEntry e) {
        super(e.id());
        this.ticketId = e.ticketId();
        this.actorId = e.actorId();
        this.actorRole = e.actorRole();
        this.action = e.action();
        this.oldValue = e.oldValue();
        this.newValue = e.newValue();
        this.details = e.details();
        this.requesterVisible = e.requesterVisible();
        this.createdAt = e.createdAt();
    }

    HistoryEntry toDomain() {
        return new HistoryEntry(getId(), ticketId, actorId, actorRole, action, oldValue, newValue, details,
                requesterVisible, createdAt);
    }
}
