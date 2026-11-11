package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.HistoryEntry;

import java.util.List;
import java.util.UUID;

/** Append-only audit (table {@code ticket_history}; the database refuses updates and deletes). */
public interface HistoryStore {

    void append(HistoryEntry entry);

    List<HistoryEntry> list(UUID ticketId, boolean requesterVisibleOnly);
}
