package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.ReopenRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReopenRequestStore {

    void save(ReopenRequest request);

    Optional<ReopenRequest> find(UUID id);

    Optional<ReopenRequest> pending(UUID ticketId);

    List<ReopenRequest> list(UUID ticketId);
}
