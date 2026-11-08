package com.maplenou.support.sla.application;

import com.maplenou.support.sla.application.port.SlaPeriodStore;
import com.maplenou.support.sla.domain.SlaPeriod;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Resolution periods of a ticket (BR-055): opened at creation and at each approved reopening. */
@Service
public class SlaPeriodService {

    private final SlaPeriodStore periods;

    public SlaPeriodService(SlaPeriodStore periods) {
        this.periods = periods;
    }

    public List<SlaPeriod> periods(UUID ticketId) {
        return periods.forTicket(ticketId).stream().sorted(Comparator.comparingInt(SlaPeriod::periodNumber)).toList();
    }

    public Optional<SlaPeriod> current(UUID ticketId) {
        return periods(ticketId).stream().filter(SlaPeriod::isOpen).max(Comparator.comparingInt(SlaPeriod::periodNumber));
    }

    public void open(UUID ticketId, String priorityCode, Instant start, Instant due) {
        int next = periods(ticketId).size() + 1;
        periods.save(new SlaPeriod(UUID.randomUUID(), ticketId, next, priorityCode, start, due, null, null, 0));
    }

    public void endCurrent(UUID ticketId, Instant at, String reason) {
        current(ticketId).ifPresent(p -> {
            p.end(at, reason);
            periods.save(p);
        });
    }

    public void recalculateCurrent(UUID ticketId, String priorityCode, Instant due) {
        current(ticketId).ifPresent(p -> {
            p.recalculate(priorityCode, due);
            periods.save(p);
        });
    }

    public void addPauseToCurrent(UUID ticketId, long minutes, Instant newDue) {
        current(ticketId).ifPresent(p -> {
            p.addPause(minutes, newDue);
            periods.save(p);
        });
    }
}
