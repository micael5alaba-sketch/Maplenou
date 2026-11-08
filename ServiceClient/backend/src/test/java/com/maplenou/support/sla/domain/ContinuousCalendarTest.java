package com.maplenou.support.sla.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** DEC-CS10-16: the SLA clock runs 24/7 — weekends and public holidays count. */
class ContinuousCalendarTest {

    private static final ZoneId LOME = ZoneId.of("Africa/Lome");
    private final BusinessCalendar cal = BusinessCalendar.continuous(LOME);

    private static Instant at(int y, int m, int d, int h, int min) {
        return ZonedDateTime.of(y, m, d, h, min, 0, 0, LOME).toInstant();
    }

    @Test
    void everyDayIsAWorkingDayIncludingWeekendsAndHolidays() {
        assertThat(cal.isBusinessDay(LocalDate.of(2026, 10, 31))).isTrue(); // Saturday
        assertThat(cal.isBusinessDay(LocalDate.of(2026, 11, 1))).isTrue();  // Sunday, Toussaint
        assertThat(cal.isContinuous()).isTrue();
    }

    @Test
    void fridayEveningPlusFourHoursIsSaturdayNight() {
        // Friday 2026-10-30 22:00 + 4 h = Saturday 02:00 (a business-hours calendar would give Monday 12:00)
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 30, 22, 0), 240)).isEqualTo(at(2026, 10, 31, 2, 0));
    }

    @Test
    void elapsedTimeIsWallClockTime() {
        assertThat(cal.businessMinutesBetween(at(2026, 10, 31, 8, 0), at(2026, 11, 1, 8, 0))).isEqualTo(24 * 60);
    }

    @Test
    void aResolutionDayIsTwentyFourHours() {
        assertThat(cal.minutesPerBusinessDay()).isEqualTo(24 * 60);
        Instant start = at(2026, 10, 31, 9, 0); // Saturday
        long high = policy("HIGH").resolutionMinutes(cal.minutesPerBusinessDay());
        assertThat(cal.plusBusinessMinutes(start, high)).isEqualTo(start.plus(Duration.ofHours(24)));
        long low = policy("LOW").resolutionMinutes(cal.minutesPerBusinessDay());
        assertThat(cal.plusBusinessMinutes(start, low)).isEqualTo(start.plus(Duration.ofDays(5)));
    }

    private static SlaPolicy policy(String code) {
        return SlaPolicy.defaults().stream().filter(p -> p.priorityCode().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void approachingAtSeventyFivePercentOfWallClockTime() {
        Instant start = at(2026, 10, 31, 0, 0);
        Instant due = cal.plusBusinessMinutes(start, 240);
        assertThat(SlaState.evaluate(cal, start, due, null, null, start.plus(Duration.ofMinutes(179))))
                .isEqualTo(SlaState.ON_TRACK);
        assertThat(SlaState.evaluate(cal, start, due, null, null, start.plus(Duration.ofMinutes(180))))
                .isEqualTo(SlaState.APPROACHING);
        assertThat(SlaState.evaluate(cal, start, due, null, null, due.plusSeconds(60))).isEqualTo(SlaState.BREACHED);
    }
}
