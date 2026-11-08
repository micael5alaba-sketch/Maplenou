package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.ExternalAction;

import java.util.List;
import java.util.UUID;

public interface ExternalActionStore {

    void save(ExternalAction action);

    List<ExternalAction> list(UUID ticketId);
}
