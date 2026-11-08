package com.maplenou.support.ticket.domain;

/** Human-readable ticket numbers: {@code TKT-2026-00125} (BR-002). */
public final class TicketNumbers {

    private TicketNumbers() {
    }

    public static String format(int year, long sequenceValue) {
        if (sequenceValue <= 0) {
            throw new IllegalArgumentException("sequence value must be positive");
        }
        return String.format("TKT-%d-%05d", year, sequenceValue);
    }
}
