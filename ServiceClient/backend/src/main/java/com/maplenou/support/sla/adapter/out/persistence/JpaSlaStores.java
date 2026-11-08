package com.maplenou.support.sla.adapter.out.persistence;

import com.maplenou.support.sla.application.port.HolidayStore;
import com.maplenou.support.sla.application.port.SlaPeriodStore;
import com.maplenou.support.sla.application.port.SlaPolicyStore;
import com.maplenou.support.sla.domain.Holiday;
import com.maplenou.support.sla.domain.SlaPeriod;
import com.maplenou.support.sla.domain.SlaPolicy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
class JpaSlaStores implements HolidayStore, SlaPolicyStore, SlaPeriodStore {

    private final HolidayJpaRepository holidays;
    private final SlaPolicyJpaRepository policies;
    private final SlaPeriodJpaRepository periods;

    JpaSlaStores(HolidayJpaRepository holidays, SlaPolicyJpaRepository policies, SlaPeriodJpaRepository periods) {
        this.holidays = holidays;
        this.policies = policies;
        this.periods = periods;
    }

    @Override
    public List<Holiday> all() {
        return holidays.findAll().stream().map(HolidayEntity::toDomain).toList();
    }

    @Override
    public void save(Holiday holiday) {
        holidays.save(new HolidayEntity(holiday));
    }

    @Override
    public void delete(LocalDate date) {
        holidays.deleteById(date);
    }

    @Override
    public List<SlaPolicy> allPolicies() {
        return policies.findAll().stream().map(SlaPolicyEntity::toDomain).toList();
    }

    @Override
    public void save(SlaPolicy policy) {
        SlaPolicyEntity e = policies.findById(policy.priorityCode()).orElseGet(() -> new SlaPolicyEntity(policy));
        e.copyFrom(policy);
        policies.save(e);
    }

    @Override
    public List<SlaPeriod> forTicket(UUID ticketId) {
        return periods.findByTicketIdOrderByPeriodNumberAsc(ticketId).stream().map(SlaPeriodEntity::toDomain).toList();
    }

    @Override
    public void save(SlaPeriod period) {
        SlaPeriodEntity e = periods.findById(period.id()).orElseGet(() -> new SlaPeriodEntity(period.id()));
        e.copyFrom(period);
        periods.save(e);
    }
}
