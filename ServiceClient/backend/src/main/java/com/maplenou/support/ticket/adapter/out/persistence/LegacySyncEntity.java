package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.LegacySyncStore.LegacyLink;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "legacy_ticket_sync")
class LegacySyncEntity {
    @Id
    @Column(name = "legacy_id")
    UUID legacyId;
    @Column(name = "ticket_id", nullable = false)
    UUID ticketId;
    @Column(name = "legacy_status")
    String legacyStatus;
    @Column(name = "synced_at", nullable = false)
    Instant syncedAt;

    protected LegacySyncEntity() {
    }

    LegacySyncEntity(LegacyLink link) {
        this.legacyId = link.legacyId();
        this.ticketId = link.ticketId();
        this.legacyStatus = link.legacyStatus();
        this.syncedAt = link.syncedAt();
    }

    LegacyLink toDomain() {
        return new LegacyLink(legacyId, ticketId, legacyStatus, syncedAt);
    }
}
