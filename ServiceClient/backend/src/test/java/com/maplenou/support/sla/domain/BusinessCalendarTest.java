package com.maplenou.support.sla.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** DEC-001 / BR-032: Monday–Friday 08:00–18:00, public holidays excluded. */
class BusinessCalendarTest {

    private static final ZoneId LOME = ZoneId.of("Africa/Lome");
    private final BusinessCalendar cal = BusinessCalendar.standard(LOME, Set.of(LocalDate.of(2026, 11, 2))::contains);

    private static Instant at(int y, int m, int d, int h, int min) {
        return ZonedDateTime.of(y, m, d, h, min, 0, 0, LOME).toInstant();
    }

    @Test
    void addsWithinTheSameDay() {
        // Thursday 2026-10-08 10:00 + 4h = 14:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 8, 10, 0), 240)).isEqualTo(at(2026, 10, 8, 14, 0));
    }

    @Test
    void rollsOverToNextBusinessDay() {
        // Thursday 16:00 + 4h → Friday 10:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 8, 16, 0), 240)).isEqualTo(at(2026, 10, 9, 10, 0));
    }

    @Test
    void startOutsideBusinessHoursStartsAtNextOpening() {
        // Thursday 20:48 + 4h → Friday 12:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 8, 20, 48), 240)).isEqualTo(at(2026, 10, 9, 12, 0));
        // Wednesday 06:00 + 1h → Wednesday 09:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 7, 6, 0), 60)).isEqualTo(at(2026, 10, 7, 9, 0));
    }

    @Test
    void skipsWeekends() {
        // Friday 17:00 + 2h → Monday 09:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 9, 17, 0), 120)).isEqualTo(at(2026, 10, 12, 9, 0));
        // Saturday + 1h → Monday 09:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 10, 11, 0), 60)).isEqualTo(at(2026, 10, 12, 9, 0));
    }

    @Test
    void skipsPublicHolidays() {
        // Friday 2026-10-30 17:00 + 2h: Monday 2026-11-02 is a holiday → Tuesday 09:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 30, 17, 0), 120)).isEqualTo(at(2026, 11, 3, 9, 0));
    }

    @Test
    void endingExactlyAtClosingStaysOnThatDay() {
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 8, 8, 0), 600)).isEqualTo(at(2026, 10, 8, 18, 0));
    }

    @Test
    void multiDayTargets() {
        // 5 business days (3000 min) from Monday 08:00 → Friday 18:00
        assertThat(cal.plusBusinessMinutes(at(2026, 10, 5, 8, 0), 3000)).isEqualTo(at(2026, 10, 9, 18, 0));
    }

    @Test
    void measuresBusinessMinutesBetween() {
        assertThat(cal.businessMinutesBetween(at(2026, 10, 8, 16, 0), at(2026, 10, 9, 10, 0))).isEqualTo(240);
        assertThat(cal.businessMinutesBetween(at(2026, 10, 9, 17, 0), at(2026, 10, 12, 9, 0))).isEqualTo(120);
        assertThat(cal.businessMinutesBetween(at(2026, 10, 10, 9, 0), at(2026, 10, 11, 17, 0))).isZero();
        assertThat(cal.businessMinutesBetween(at(2026, 10, 9, 10, 0), at(2026, 10, 8, 10, 0))).isZero();
    }

    @Test
    void addAndMeasureAreConsistent() {
        Instant start = at(2026, 10, 8, 13, 17);
        for (long minutes : new long[]{1, 59, 240, 600, 1440, 3000}) {
            Instant end = cal.plusBusinessMinutes(start, minutes);
            assertThat(cal.businessMinutesBetween(start, end)).as("minutes=%d", minutes).isEqualTo(minutes);
        }
    }
}
