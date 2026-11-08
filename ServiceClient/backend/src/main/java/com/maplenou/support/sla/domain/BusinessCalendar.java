package com.maplenou.support.sla.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Calendar used for SLA computation. Two kinds (DEC-CS10-16):
 * <ul>
 *   <li>{@link #continuous(ZoneId)} — the default since 2026-10-29: every minute of every day counts,
 *       weekends and public holidays included (24/7);</li>
 *   <li>business hours (DEC-001, BR-032): working days between opening and closing, excluding holidays.</li>
 * </ul>
 * Pure value object: no framework dependency, fully unit-testable.
 */
public final class BusinessCalendar {

    private static final int MAX_DAYS_SCANNED = 3660;

    private final ZoneId zone;
    private final LocalTime opening;
    private final LocalTime closing;
    private final Set<DayOfWeek> workingDays;
    private final Predicate<LocalDate> holiday;
    private final boolean continuous;

    public BusinessCalendar(ZoneId zone, LocalTime opening, LocalTime closing,
                            Set<DayOfWeek> workingDays, Predicate<LocalDate> holiday) {
        this.zone = Objects.requireNonNull(zone);
        this.opening = Objects.requireNonNull(opening);
        this.closing = Objects.requireNonNull(closing);
        if (!opening.isBefore(closing)) {
            throw new IllegalArgumentException("opening must be before closing");
        }
        this.workingDays = EnumSet.copyOf(workingDays);
        this.holiday = holiday == null ? d -> false : holiday;
        this.continuous = false;
    }

    private BusinessCalendar(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone);
        this.opening = LocalTime.MIDNIGHT;
        this.closing = LocalTime.MAX;
        this.workingDays = EnumSet.allOf(DayOfWeek.class);
        this.holiday = d -> false;
        this.continuous = true;
    }

    /** 24/7: every day, holidays included, all day long. A "day" in resolution targets is 24 hours. */
    public static BusinessCalendar continuous(ZoneId zone) {
        return new BusinessCalendar(zone);
    }

    public boolean isContinuous() {
        return continuous;
    }

    /** Monday–Friday 08:00–18:00 with the given holidays. */
    public static BusinessCalendar standard(ZoneId zone, Predicate<LocalDate> holiday) {
        return new BusinessCalendar(zone, LocalTime.of(8, 0), LocalTime.of(18, 0),
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), holiday);
    }

    public ZoneId zone() {
        return zone;
    }

    /** Business minutes in one full working day (600 for 08:00–18:00). */
    public long minutesPerBusinessDay() {
        if (continuous) {
            return 24 * 60;
        }
        return Duration.between(opening, closing).toMinutes();
    }

    public boolean isBusinessDay(LocalDate date) {
        return continuous || workingDays.contains(date.getDayOfWeek()) && !holiday.test(date);
    }

    /** Adds {@code minutes} of business time to {@code start}. */
    public Instant plusBusinessMinutes(Instant start, long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("minutes must be >= 0");
        }
        if (minutes == 0) {
            return start;
        }
        if (continuous) {
            return start.plus(Duration.ofMinutes(minutes));
        }
        ZonedDateTime cursor = start.atZone(zone);
        long remaining = minutes;
        for (int i = 0; i < MAX_DAYS_SCANNED; i++) {
            LocalDate day = cursor.toLocalDate();
            ZonedDateTime dayOpen = day.atTime(opening).atZone(zone);
            ZonedDateTime dayClose = day.atTime(closing).atZone(zone);
            if (!isBusinessDay(day) || !cursor.isBefore(dayClose)) {
                cursor = day.plusDays(1).atTime(opening).atZone(zone);
                continue;
            }
            if (cursor.isBefore(dayOpen)) {
                cursor = dayOpen;
            }
            long available = Duration.between(cursor, dayClose).toMinutes();
            if (remaining <= available) {
                return cursor.plusMinutes(remaining).toInstant();
            }
            remaining -= available;
            cursor = day.plusDays(1).atTime(opening).atZone(zone);
        }
        throw new IllegalStateException("No business time found within " + MAX_DAYS_SCANNED + " days");
    }

    /** Business minutes elapsed between two instants (0 when {@code to} is not after {@code from}). */
    public long businessMinutesBetween(Instant from, Instant to) {
        if (!to.isAfter(from)) {
            return 0;
        }
        if (continuous) {
            return Duration.between(from, to).toMinutes();
        }
        ZonedDateTime start = from.atZone(zone);
        ZonedDateTime end = to.atZone(zone);
        long total = 0;
        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        for (int i = 0; !day.isAfter(last) && i < MAX_DAYS_SCANNED; i++, day = day.plusDays(1)) {
            if (!isBusinessDay(day)) {
                continue;
            }
            ZonedDateTime open = day.atTime(opening).atZone(zone);
            ZonedDateTime close = day.atTime(closing).atZone(zone);
            ZonedDateTime s = start.isAfter(open) ? start : open;
            ZonedDateTime e = end.isBefore(close) ? end : close;
            if (e.isAfter(s)) {
                total += Duration.between(s, e).toMinutes();
            }
        }
        return total;
    }
}
