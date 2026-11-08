package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.Satisfaction;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Customer satisfaction ratings (table {@code ticket_satisfaction}). */
public interface FeedbackStore {

    record Stats(long count, Double average, Double satisfiedPercent) {
    }

    Optional<Satisfaction> find(UUID ticketId);

    void save(Satisfaction satisfaction);

    /** Ratings given in [from, to). */
    Stats stats(Instant from, Instant to);
}
