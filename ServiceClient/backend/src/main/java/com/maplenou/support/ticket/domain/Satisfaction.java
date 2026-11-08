package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.SupportException;

import java.time.Instant;
import java.util.UUID;

/** The requester's rating of the answer, 1 (very unhappy) to 5 (very happy), given once (DEC-CS11-05). */
public record Satisfaction(UUID ticketId, int rating, String comment, UUID ratedBy, Instant ratedAt) {

    public Satisfaction {
        if (rating < 1 || rating > 5) {
            throw SupportException.badRequest("INVALID_RATING", "La note doit être comprise entre 1 et 5.", null);
        }
    }

    /** 4 or 5 counts as satisfied in the reports. */
    public boolean satisfied() {
        return rating >= 4;
    }
}
