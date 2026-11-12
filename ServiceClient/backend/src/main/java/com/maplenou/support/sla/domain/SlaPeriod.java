package com.maplenou.support.sla.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * One resolution-SLA period of a ticket. Reopening ends the previous period and opens a new one (BR-055);
 * {@code pausedMinutes} adds up the time the clock was stopped (DEC-CS11-03).
 */
public final class SlaPeriod {

    private final UUID id;
    private final UUID ticketId;
    private final int periodNumber;
    private String priorityCode;
    private final Instant startedAt;
    private Instant resolutionDueAt;
    private Instant endedAt;
    private String endReason;
    private long pausedMinutes;

    public SlaPeriod(UUID id, UUID ticketId, int periodNumber, String priorityCode, Instant startedAt,
                     Instant resolutionDueAt, Instant endedAt, String endReason, long pausedMinutes) {
        this.id = id;
        this.ticketId = ticketId;
        this.periodNumber = periodNumber;
        this.priorityCode = priorityCode;
        this.startedAt = startedAt;
        this.resolutionDueAt = resolutionDueAt;
        this.endedAt = endedAt;
        this.endReason = endReason;
        this.pausedMinutes = pausedMinutes;
    }

    public void recalculate(String priorityCode, Instant due) {
        this.priorityCode = priorityCode;
        this.resolutionDueAt = due;
    }

    public void addPause(long minutes, Instant newDue) {
        this.pausedMinutes += Math.max(0, minutes);
        this.resolutionDueAt = newDue;
    }

    public void end(Instant at, String reason) {
        this.endedAt = at;
        this.endReason = reason;
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    public UUID id() {
        return id;
    }

    public UUID ticketId() {
        return ticketId;
    }

    public int periodNumber() {
        return periodNumber;
    }

    public String priorityCode() {
        return priorityCode;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant resolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    public String endReason() {
        return endReason;
    }

    public long pausedMinutes() {
        return pausedMinutes;
    }
}
