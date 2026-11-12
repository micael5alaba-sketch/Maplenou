package com.maplenou.support.sla.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Targets for one priority (DEC-001, table {@code sla_policies}). The resolution target is either a number of
 * minutes or a number of calendar days ({@code resolutionDays} × length of a day of the calendar: 24 h in 24/7
 * mode, the opening hours in business-hours mode).
 */
public record SlaPolicy(String priorityCode, int firstResponseMinutes, Integer resolutionMinutes,
                        Integer resolutionDays) {

    public long resolutionMinutes(long minutesPerDay) {
        return resolutionMinutes != null ? resolutionMinutes : (long) resolutionDays * minutesPerDay;
    }

    /** Targets delivered with the product (DEC-001). */
    public static List<SlaPolicy> defaults() {
        return List.of(new SlaPolicy("LOW", 24 * 60, null, 5), new SlaPolicy("MEDIUM", 8 * 60, null, 3),
                new SlaPolicy("HIGH", 4 * 60, null, 1), new SlaPolicy("CRITICAL", 60, 4 * 60, null));
    }

    public List<String> problems() {
        List<String> p = new ArrayList<>();
        if (firstResponseMinutes < 1 || firstResponseMinutes > 60 * 24 * 30) {
            p.add("Première réponse : entre 1 minute et 30 jours (" + priorityCode + ").");
        }
        if ((resolutionMinutes == null) == (resolutionDays == null)) {
            p.add("Résolution : indiquez soit des minutes, soit des jours (" + priorityCode + ").");
        }
        if (resolutionMinutes != null && (resolutionMinutes < 1 || resolutionMinutes > 60 * 24 * 90)) {
            p.add("Résolution : entre 1 minute et 90 jours (" + priorityCode + ").");
        }
        if (resolutionDays != null && (resolutionDays < 1 || resolutionDays > 90)) {
            p.add("Résolution : entre 1 et 90 jours (" + priorityCode + ").");
        }
        return p;
    }
}
