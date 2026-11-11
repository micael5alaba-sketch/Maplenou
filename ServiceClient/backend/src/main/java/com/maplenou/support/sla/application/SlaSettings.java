package com.maplenou.support.sla.application;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * How deadlines are counted (built from the configuration by the adapter layer).
 *
 * @param continuous true = 24/7, weekends and holidays count (DEC-CS10-16); false = business hours minus holidays
 * @param cacheTtl   how long holidays and SLA targets stay cached
 */
public record SlaSettings(ZoneId zone, LocalTime opening, LocalTime closing, boolean continuous, Duration cacheTtl) {
}
