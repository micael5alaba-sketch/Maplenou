package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.AiSuggestion;

import java.util.Optional;
import java.util.UUID;

public interface AiSuggestionStore {

    Optional<AiSuggestion> findSuggestion(UUID ticketId);

    void save(AiSuggestion suggestion);
}
