package com.maplenou.support.sla.application.port;

import com.maplenou.support.sla.domain.SlaPeriod;

import java.util.List;
import java.util.UUID;

/** Resolution periods of tickets (table {@code ticket_sla_periods}). */
public interface SlaPeriodStore {

    List<SlaPeriod> forTicket(UUID ticketId);

    void save(SlaPeriod period);
}
