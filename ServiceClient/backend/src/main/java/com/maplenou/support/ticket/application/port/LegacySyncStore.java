package com.maplenou.support.ticket.application.port;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Link between an imported legacy ticket and its copy (table {@code legacy_ticket_sync}, V6). */
public interface LegacySyncStore {

    record LegacyLink(UUID legacyId, UUID ticketId, String legacyStatus, Instant syncedAt) {
    }

    /** The links of the given legacy ids, in one query (a page of the legacy source at a time). */
    Map<UUID, LegacyLink> find(Collection<UUID> legacyIds);

    void save(LegacyLink link);
}
