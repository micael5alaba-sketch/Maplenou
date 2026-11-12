package com.maplenou.support.sla.application;

import com.maplenou.support.sla.application.port.HolidayStore;
import com.maplenou.support.sla.application.port.SlaPolicyStore;
import com.maplenou.support.sla.domain.BusinessCalendar;
import com.maplenou.support.sla.domain.Holiday;
import com.maplenou.support.sla.domain.SlaPolicy;
import com.maplenou.support.sla.domain.SlaState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes SLA targets and states (FR-016) from the business calendar and the targets per priority. Public API of
 * the SLA module: the ticket module only passes instants and priority codes.
 */
@Service
public class SlaService {

    private final HolidayStore holidays;
    private final SlaPolicyStore policies;
    private final SlaSettings settings;
    private final Clock clock;

    private volatile BusinessCalendar cachedCalendar;
    private volatile Map<String, SlaPolicy> cachedPolicies;
    private volatile Instant cachedAt = Instant.EPOCH;

    public SlaService(HolidayStore holidays, SlaPolicyStore policies, SlaSettings settings, Clock clock) {
        this.holidays = holidays;
        this.policies = policies;
        this.settings = settings;
        this.clock = clock;
    }

    /** How deadlines are counted, for the administration screens. */
    public record CalendarSettings(String mode, String zone, LocalTime opening, LocalTime closing, long minutesPerDay) {
    }

    public CalendarSettings settings() {
        BusinessCalendar cal = calendar();
        boolean c = settings.continuous();
        return new CalendarSettings(c ? "always" : "business-hours", settings.zone().getId(),
                c ? null : settings.opening(), c ? null : settings.closing(), cal.minutesPerBusinessDay());
    }

    public BusinessCalendar calendar() {
        refreshIfStale();
        return cachedCalendar;
    }

    public SlaPolicy policy(String priorityCode) {
        refreshIfStale();
        SlaPolicy p = cachedPolicies.get(priorityCode);
        if (p == null) {
            throw new IllegalStateException("No SLA policy for priority " + priorityCode);
        }
        return p;
    }

    public List<SlaPolicy> policies() {
        refreshIfStale();
        return List.copyOf(cachedPolicies.values());
    }

    public Instant firstResponseDue(Instant start, String priorityCode) {
        return calendar().plusBusinessMinutes(start, policy(priorityCode).firstResponseMinutes());
    }

    public Instant resolutionDue(Instant start, String priorityCode) {
        BusinessCalendar cal = calendar();
        return cal.plusBusinessMinutes(start, policy(priorityCode).resolutionMinutes(cal.minutesPerBusinessDay()));
    }

    public SlaState state(Instant start, Instant due, Instant completedAt) {
        return state(start, due, completedAt, null);
    }

    public SlaState state(Instant start, Instant due, Instant completedAt, Instant pausedSince) {
        return SlaState.evaluate(calendar(), start, due, completedAt, pausedSince, clock.instant());
    }

    /** Result of resuming a paused clock: the paused (business) minutes and the deadline pushed back by them. */
    public record Extension(long pausedMinutes, Instant newDue) {
    }

    public Extension extendAfterPause(Instant due, Instant pausedSince, Instant resumedAt) {
        BusinessCalendar cal = calendar();
        long paused = pausedSince == null || !resumedAt.isAfter(pausedSince) ? 0
                : cal.businessMinutesBetween(pausedSince, resumedAt);
        return new Extension(paused, paused == 0 ? due : cal.plusBusinessMinutes(due, paused));
    }

    /** Forces the holidays and targets to be reloaded (after an administrator change). */
    public void invalidate() {
        cachedAt = Instant.EPOCH;
        // also after commit: a request running meanwhile could have re-read the old values
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cachedAt = Instant.EPOCH;
                }
            });
        }
    }

    private void refreshIfStale() {
        Instant now = clock.instant();
        if (cachedCalendar != null && cachedPolicies != null && cachedAt.plus(settings.cacheTtl()).isAfter(now)) {
            return;
        }
        Map<String, SlaPolicy> loaded = policies.allPolicies().stream()
                .collect(Collectors.toMap(SlaPolicy::priorityCode, Function.identity()));
        BusinessCalendar cal;
        if (settings.continuous()) {
            cal = BusinessCalendar.continuous(settings.zone()); // holidays and opening hours do not apply (24/7)
        } else {
            Set<LocalDate> days = holidays.all().stream().map(Holiday::date).collect(Collectors.toSet());
            cal = new BusinessCalendar(settings.zone(), settings.opening(), settings.closing(),
                    EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), days::contains);
        }
        cachedPolicies = loaded;
        cachedCalendar = cal;
        cachedAt = now;
    }
}
