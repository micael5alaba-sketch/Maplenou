package com.maplenou.support.sla.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.sla.domain.Holiday;
import com.maplenou.support.sla.domain.SlaPeriod;
import com.maplenou.support.sla.domain.SlaPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ticket_sla_periods")
class SlaPeriodEntity extends BaseEntity {
    @Column(name = "ticket_id", nullable = false, updatable = false)
    UUID ticketId;
    @Column(name = "period_number", nullable = false, updatable = false)
    short periodNumber;
    @Column(nullable = false)
    String priority;
    @Column(name = "started_at", nullable = false, updatable = false)
    Instant startedAt;
    @Column(name = "resolution_due_at", nullable = false)
    Instant resolutionDueAt;
    @Column(name = "ended_at")
    Instant endedAt;
    @Column(name = "end_reason")
    String endReason;
    @Column(name = "paused_minutes", nullable = false)
    long pausedMinutes;

    protected SlaPeriodEntity() {
    }

    SlaPeriodEntity(UUID id) {
        super(id);
    }

    void copyFrom(SlaPeriod p) {
        this.ticketId = p.ticketId();
        this.periodNumber = (short) p.periodNumber();
        this.priority = p.priorityCode();
        this.startedAt = p.startedAt();
        this.resolutionDueAt = p.resolutionDueAt();
        this.endedAt = p.endedAt();
        this.endReason = p.endReason();
        this.pausedMinutes = p.pausedMinutes();
    }

    SlaPeriod toDomain() {
        return new SlaPeriod(getId(), ticketId, periodNumber, priority, startedAt, resolutionDueAt, endedAt, endReason,
                pausedMinutes);
    }
}

@Entity
@Table(name = "business_holidays")
class HolidayEntity {
    @Id
    @Column(name = "holiday_date")
    LocalDate date;
    @Column(nullable = false)
    String label;

    protected HolidayEntity() {
    }

    HolidayEntity(Holiday h) {
        this.date = h.date();
        this.label = h.label();
    }

    Holiday toDomain() {
        return new Holiday(date, label);
    }
}

@Entity
@Table(name = "sla_policies")
class SlaPolicyEntity {
    @Id
    @Column(name = "priority_code")
    String priorityCode;
    @Column(name = "first_response_minutes", nullable = false)
    int firstResponseMinutes;
    @Column(name = "resolution_minutes")
    Integer resolutionMinutes;
    @Column(name = "resolution_days")
    Integer resolutionDays;

    protected SlaPolicyEntity() {
    }

    SlaPolicyEntity(SlaPolicy p) {
        this.priorityCode = p.priorityCode();
        copyFrom(p);
    }

    void copyFrom(SlaPolicy p) {
        this.firstResponseMinutes = p.firstResponseMinutes();
        this.resolutionMinutes = p.resolutionMinutes();
        this.resolutionDays = p.resolutionDays();
    }

    SlaPolicy toDomain() {
        return new SlaPolicy(priorityCode, firstResponseMinutes, resolutionMinutes, resolutionDays);
    }
}
