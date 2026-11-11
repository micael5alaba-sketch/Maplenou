package com.maplenou.support.sla.domain;

import java.time.Instant;

/** SLA state shown to support users (CS-02 screen-behavior: on track, approaching, breached; plus met and paused). */
public enum SlaState {
    ON_TRACK,
    APPROACHING,
    BREACHED,
    MET,
    /** The resolution clock is stopped while the ticket waits for the requester or a third party (DEC-CS11-03). */
    PAUSED;

    /** Share of the target after which a running SLA is considered "approaching". */
    public static final int APPROACHING_PERCENT = 75;

    /**
     * Evaluates one SLA clock.
     *
     * @param start       start of the clock (ticket creation or reopening)
     * @param due         target instant
     * @param completedAt when the required action happened, or {@code null}
     * @param pausedSince when the clock was stopped, or {@code null} when it runs
     * @param now         evaluation instant
     */
    public static SlaState evaluate(BusinessCalendar calendar, Instant start, Instant due, Instant completedAt,
                                    Instant pausedSince, Instant now) {
        if (completedAt != null) {
            return completedAt.isAfter(due) ? BREACHED : MET;
        }
        if (pausedSince != null) {
            return pausedSince.isAfter(due) ? BREACHED : PAUSED;
        }
        if (now.isAfter(due)) {
            return BREACHED;
        }
        long total = calendar.businessMinutesBetween(start, due);
        long elapsed = calendar.businessMinutesBetween(start, now);
        if (total > 0 && elapsed * 100 >= (long) APPROACHING_PERCENT * total) {
            return APPROACHING;
        }
        return ON_TRACK;
    }

    /** The most urgent of two states (BREACHED > APPROACHING > ON_TRACK > PAUSED > MET). */
    public static SlaState worst(SlaState a, SlaState b) {
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(SlaState s) {
        return switch (s) {
            case BREACHED -> 4;
            case APPROACHING -> 3;
            case ON_TRACK -> 2;
            case PAUSED -> 1;
            case MET -> 0;
        };
    }
}
