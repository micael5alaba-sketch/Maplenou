package com.maplenou.support.sla.domain;

import java.time.LocalDate;

/** Officially recognized public holiday excluded from SLA time in business-hours mode (BR-032). */
public record Holiday(LocalDate date, String label) {
}
