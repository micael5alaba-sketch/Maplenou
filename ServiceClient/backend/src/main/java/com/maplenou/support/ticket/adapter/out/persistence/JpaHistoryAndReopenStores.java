package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.HistoryStore;
import com.maplenou.support.ticket.application.port.ReopenRequestStore;
import com.maplenou.support.ticket.domain.HistoryEntry;
import com.maplenou.support.ticket.domain.ReopenRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaHistoryStore implements HistoryStore {

    private final TicketHistoryJpaRepository repo;

    JpaHistoryStore(TicketHistoryJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void append(HistoryEntry entry) {
        repo.save(new TicketHistoryEntity(entry));
    }

    @Override
    public List<HistoryEntry> list(UUID ticketId, boolean requesterVisibleOnly) {
        return (requesterVisibleOnly ? repo.findByTicketIdAndRequesterVisibleTrueOrderByCreatedAtAsc(ticketId)
                : repo.findByTicketIdOrderByCreatedAtAsc(ticketId)).stream().map(TicketHistoryEntity::toDomain).toList();
    }
}

@Component
class JpaReopenRequestStore implements ReopenRequestStore {

    private final ReopenRequestJpaRepository repo;

    JpaReopenRequestStore(ReopenRequestJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(ReopenRequest request) {
        ReopenRequestEntity e = repo.findById(request.id()).orElseGet(() -> new ReopenRequestEntity(request.id()));
        e.copyFrom(request);
        repo.save(e);
    }

    @Override
    public Optional<ReopenRequest> find(UUID id) {
        return repo.findById(id).map(ReopenRequestEntity::toDomain);
    }

    @Override
    public Optional<ReopenRequest> pending(UUID ticketId) {
        return repo.findByTicketIdAndStatus(ticketId, ReopenRequest.Status.PENDING).map(ReopenRequestEntity::toDomain);
    }

    @Override
    public List<ReopenRequest> list(UUID ticketId) {
        return repo.findByTicketIdOrderByCreatedAtAsc(ticketId).stream().map(ReopenRequestEntity::toDomain).toList();
    }
}
