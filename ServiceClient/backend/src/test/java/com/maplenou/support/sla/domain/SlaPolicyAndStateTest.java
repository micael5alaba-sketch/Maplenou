package com.maplenou.support.sla.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SlaPolicyAndStateTest {

    private static final ZoneId LOME = ZoneId.of("Africa/Lome");
    private final BusinessCalendar cal = BusinessCalendar.standard(LOME, (LocalDate d) -> false);

    private static Instant at(int d, int h, int m) {
        return ZonedDateTime.of(2026, 10, d, h, m, 0, 0, LOME).toInstant();
    }

    private static SlaPolicy policy(String code) {
        return SlaPolicy.defaults().stream().filter(p -> p.priorityCode().equals(code)).findFirst().orElseThrow();
    }

    @Test
    void approvedTargetsDec001() {
        assertThat(policy("LOW").firstResponseMinutes()).isEqualTo(24 * 60);
        assertThat(policy("MEDIUM").firstResponseMinutes()).isEqualTo(8 * 60);
        assertThat(policy("HIGH").firstResponseMinutes()).isEqualTo(4 * 60);
        assertThat(policy("CRITICAL").firstResponseMinutes()).isEqualTo(60);
        assertThat(policy("LOW").resolutionMinutes(600)).isEqualTo(5 * 600);
        assertThat(policy("MEDIUM").resolutionMinutes(600)).isEqualTo(3 * 600);
        assertThat(policy("HIGH").resolutionMinutes(600)).isEqualTo(600);
        assertThat(policy("CRITICAL").resolutionMinutes(600)).isEqualTo(4 * 60);
    }

    @Test
    void policiesAreValidated() {
        assertThat(SlaPolicy.defaults()).allSatisfy(p -> assertThat(p.problems()).isEmpty());
        assertThat(new SlaPolicy("X", 60, 120, 2).problems()).isNotEmpty();   // both minutes and days
        assertThat(new SlaPolicy("X", 60, null, null).problems()).isNotEmpty(); // none
        assertThat(new SlaPolicy("X", 0, 60, null).problems()).isNotEmpty();
    }

    @Test
    void states() {
        Instant start = at(8, 8, 0);
        Instant due = at(8, 12, 0); // 4 business hours
        assertThat(SlaState.evaluate(cal, start, due, null, null, at(8, 9, 0))).isEqualTo(SlaState.ON_TRACK);
        assertThat(SlaState.evaluate(cal, start, due, null, null, at(8, 11, 0))).isEqualTo(SlaState.APPROACHING);
        assertThat(SlaState.evaluate(cal, start, due, null, null, at(8, 12, 1))).isEqualTo(SlaState.BREACHED);
        assertThat(SlaState.evaluate(cal, start, due, at(8, 10, 0), null, at(8, 15, 0))).isEqualTo(SlaState.MET);
        assertThat(SlaState.evaluate(cal, start, due, at(8, 13, 0), null, at(8, 15, 0))).isEqualTo(SlaState.BREACHED);
    }

    @Test
    void pausedClockIsNeitherRunningNorLate() {
        Instant start = at(8, 8, 0);
        Instant due = at(8, 12, 0);
        // paused before the deadline: still PAUSED even if "now" is long after the deadline (DEC-CS11-03)
        assertThat(SlaState.evaluate(cal, start, due, null, at(8, 10, 0), at(9, 15, 0))).isEqualTo(SlaState.PAUSED);
        // paused after the deadline: it was already late
        assertThat(SlaState.evaluate(cal, start, due, null, at(8, 13, 0), at(9, 15, 0))).isEqualTo(SlaState.BREACHED);
    }

    @Test
    void worstStateWins() {
        assertThat(SlaState.worst(SlaState.MET, SlaState.APPROACHING)).isEqualTo(SlaState.APPROACHING);
        assertThat(SlaState.worst(SlaState.BREACHED, SlaState.ON_TRACK)).isEqualTo(SlaState.BREACHED);
        assertThat(SlaState.worst(SlaState.PAUSED, SlaState.MET)).isEqualTo(SlaState.PAUSED);
        assertThat(SlaState.worst(SlaState.PAUSED, SlaState.ON_TRACK)).isEqualTo(SlaState.ON_TRACK);
    }
}
