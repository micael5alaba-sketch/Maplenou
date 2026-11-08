package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.sla.application.SlaService;
import com.maplenou.support.sla.domain.BusinessCalendar;
import com.maplenou.support.ticket.application.port.FeedbackStore;
import com.maplenou.support.ticket.application.port.TicketSearch;
import com.maplenou.support.ticket.application.view.TicketViews.TicketSummary;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.Ticket;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Figures about tickets for the support dashboard (screen B1) and the basic report (functional design §18).
 * Public API used by the report module.
 */
@Service
@Transactional(readOnly = true)
public class TicketStatistics {

    public record CategoryCount(String code, String name, long count) {
    }

    public record Satisfaction(long count, Double average, Double satisfiedPercent) {
    }

    public record Dashboard(long openCount, long newCount, long unassignedCount, long inProgressCount,
                            long waitingForCustomerCount, long resolvedTodayCount, long escalatedOpenCount,
                            long slaBreachedOpenCount, long myOpenCount, Map<String, Long> openByPriority,
                            List<CategoryCount> openByCategory, List<TicketSummary> recent,
                            Satisfaction satisfactionLast30Days) {
    }

    public record Report(LocalDate from, LocalDate to, long created, long resolved, long stillOpen,
                         Double avgFirstResponseBusinessMinutes, Double avgResolutionBusinessMinutes,
                         Double firstResponseSlaMetPercent, Double resolutionSlaMetPercent,
                         List<CategoryCount> byCategory, Map<LocalDate, Long> createdPerDay,
                         Satisfaction satisfaction) {
    }

    private final TicketSearch search;
    private final TicketQueryService queries;
    private final VocabularyService vocabulary;
    private final CategoryService categories;
    private final FeedbackStore feedback;
    private final SlaService sla;
    private final Clock clock;

    public TicketStatistics(TicketSearch search, TicketQueryService queries, VocabularyService vocabulary,
                            CategoryService categories, FeedbackStore feedback, SlaService sla, Clock clock) {
        this.search = search;
        this.queries = queries;
        this.vocabulary = vocabulary;
        this.categories = categories;
        this.feedback = feedback;
        this.sla = sla;
        this.clock = clock;
    }

    public Dashboard dashboard(Actor user) {
        Instant now = clock.instant();
        ZoneId zone = sla.calendar().zone();
        Instant startOfDay = LocalDate.now(clock.withZone(zone)).atStartOfDay(zone).toInstant();
        Workflow wf = vocabulary.workflow();
        Set<String> open = wf.openCodes();
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (PriorityDefinition p : vocabulary.priorities().all()) {
            byPriority.put(p.code(), 0L);
        }
        search.countByPriority(open).forEach((code, count) -> byPriority.put(code, count));
        List<CategoryCount> byCategory = categoryCounts(search.countByCategory(open));
        var recent = queries.search(user, TicketSearchCriteria.openTickets(), new PageQuery(0, 5), "createdAt,desc");
        FeedbackStore.Stats csat = feedback.stats(now.minus(30, ChronoUnit.DAYS), now);
        return new Dashboard(
                search.countWithStatus(open),
                search.countWithStatus(wf.codesOfKind(StatusKind.NEW)),
                search.countUnassigned(open),
                search.countWithStatus(wf.codesOfKind(StatusKind.IN_PROGRESS)),
                search.countWithStatus(wf.codesOfKind(StatusKind.WAITING_REQUESTER)),
                search.countResolvedSince(startOfDay),
                search.countEscalated(open),
                search.countBreached(open, now),
                search.countAssignedTo(user.id(), open),
                byPriority, byCategory, recent.items(),
                new Satisfaction(csat.count(), csat.average(), csat.satisfiedPercent()));
    }

    private List<CategoryCount> categoryCounts(Map<String, Long> counts) {
        List<CategoryCount> list = new ArrayList<>();
        for (Category c : categories.all()) {
            Long n = counts.get(c.code());
            if (n != null) {
                list.add(new CategoryCount(c.code(), c.name(), n));
            }
        }
        return list;
    }

    /** Basic metrics over tickets created in [from, to]; not advanced analytics (CS-01 exclusions). */
    public Report report(LocalDate from, LocalDate to) {
        ZoneId zone = sla.calendar().zone();
        BusinessCalendar cal = sla.calendar();
        Workflow wf = vocabulary.workflow();
        Instant start = from.atStartOfDay(zone).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(zone).toInstant();
        List<Ticket> list = search.createdBetween(start, end);
        long resolved = list.stream().filter(t -> t.resolvedAt() != null).count();
        long open = list.stream().filter(t -> wf.isOpen(t.statusCode())).count();
        OptionalDouble firstResp = list.stream().filter(t -> t.firstRespondedAt() != null)
                .mapToLong(t -> cal.businessMinutesBetween(t.createdAt(), t.firstRespondedAt())).average();
        OptionalDouble resolution = list.stream().filter(t -> t.resolvedAt() != null)
                .mapToLong(t -> cal.businessMinutesBetween(t.createdAt(), t.resolvedAt())).average();
        long frTotal = list.stream().filter(t -> t.firstRespondedAt() != null).count();
        long frMet = list.stream().filter(t -> t.firstRespondedAt() != null
                && !t.firstRespondedAt().isAfter(t.firstResponseDueAt())).count();
        long resMet = list.stream().filter(t -> t.resolvedAt() != null
                && !t.resolvedAt().isAfter(t.resolutionDueAt())).count();
        Map<String, Long> perCategory = new LinkedHashMap<>();
        list.forEach(t -> perCategory.merge(t.categoryCode(), 1L, Long::sum));
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            perDay.put(d, 0L);
        }
        list.forEach(t -> perDay.merge(t.createdAt().atZone(zone).toLocalDate(), 1L, Long::sum));
        FeedbackStore.Stats csat = feedback.stats(start, end);
        return new Report(from, to, list.size(), resolved, open,
                firstResp.isPresent() ? round(firstResp.getAsDouble()) : null,
                resolution.isPresent() ? round(resolution.getAsDouble()) : null,
                frTotal == 0 ? null : round(100.0 * frMet / frTotal),
                resolved == 0 ? null : round(100.0 * resMet / resolved),
                categoryCounts(perCategory), perDay,
                new Satisfaction(csat.count(), csat.average(), csat.satisfiedPercent()));
    }

    /** Number of tickets, whatever their status. */
    public long totalTickets() {
        return search.countWithStatus(vocabulary.workflow().statuses().stream()
                .map(com.maplenou.support.ticket.domain.vocabulary.StatusDefinition::code).toList());
    }

    /** Open tickets per agent (assignment dropdown and agents page). */
    public Map<UUID, Long> openLoadByAgent() {
        return search.openLoadByAgent(vocabulary.workflow().openCodes());
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
