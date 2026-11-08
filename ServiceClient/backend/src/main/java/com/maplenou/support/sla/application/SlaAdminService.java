package com.maplenou.support.sla.application;

import com.maplenou.support.shared.SupportException;
import com.maplenou.support.sla.application.port.HolidayStore;
import com.maplenou.support.sla.application.port.SlaPolicyStore;
import com.maplenou.support.sla.domain.Holiday;
import com.maplenou.support.sla.domain.SlaPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Business calendar and SLA targets, edited by administrators (DEC-001, BR-065). */
@Service
public class SlaAdminService {

    private final HolidayStore holidays;
    private final SlaPolicyStore policies;
    private final SlaService sla;

    public SlaAdminService(HolidayStore holidays, SlaPolicyStore policies, SlaService sla) {
        this.holidays = holidays;
        this.policies = policies;
        this.sla = sla;
    }

    @Transactional(readOnly = true)
    public List<Holiday> holidays() {
        return holidays.all().stream().sorted(Comparator.comparing(Holiday::date)).toList();
    }

    @Transactional
    public Holiday putHoliday(LocalDate date, String label) {
        Holiday h = new Holiday(date, label.trim());
        holidays.save(h);
        sla.invalidate();
        return h;
    }

    @Transactional
    public void deleteHoliday(LocalDate date) {
        holidays.delete(date);
        sla.invalidate();
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> policies() {
        return policies.allPolicies();
    }

    @Transactional
    public SlaPolicy updatePolicy(SlaPolicy policy) {
        List<String> problems = policy.problems();
        if (!problems.isEmpty()) {
            throw SupportException.businessRule("INVALID_SLA_POLICY", String.join(" ", problems), "DEC-001");
        }
        if (policies.allPolicies().stream().noneMatch(p -> p.priorityCode().equals(policy.priorityCode()))) {
            throw SupportException.notFound("Priorité");
        }
        policies.save(policy);
        sla.invalidate();
        return policy;
    }

    /** Gives a new priority the targets of {@code template} (or 8 h / 3 days) until an administrator sets them. */
    @Transactional
    public void ensurePolicy(String priorityCode, String templateCode) {
        List<SlaPolicy> all = policies.allPolicies();
        if (all.stream().anyMatch(p -> p.priorityCode().equals(priorityCode))) {
            return;
        }
        SlaPolicy base = all.stream().filter(p -> p.priorityCode().equals(templateCode)).findFirst()
                .orElse(new SlaPolicy(priorityCode, 8 * 60, null, 3));
        policies.save(new SlaPolicy(priorityCode, base.firstResponseMinutes(), base.resolutionMinutes(),
                base.resolutionDays()));
        sla.invalidate();
    }
}
