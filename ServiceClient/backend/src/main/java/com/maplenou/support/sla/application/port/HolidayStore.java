package com.maplenou.support.sla.application.port;

import com.maplenou.support.sla.domain.Holiday;

import java.time.LocalDate;
import java.util.List;

/** Public holidays of the business calendar (table {@code business_holidays}). */
public interface HolidayStore {

    List<Holiday> all();

    void save(Holiday holiday);

    void delete(LocalDate date);
}
