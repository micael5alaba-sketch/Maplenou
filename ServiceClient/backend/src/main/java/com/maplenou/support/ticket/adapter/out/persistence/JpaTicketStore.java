package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import com.maplenou.support.ticket.application.TicketSearchCriteria;
import com.maplenou.support.ticket.application.port.TicketSearch;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.domain.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Ticket persistence: maps the plain aggregate to its JPA model and back. */
/**
 * Every call runs in a transaction (joining the caller's one when there is one) so that the lazy collections of
 * {@link TicketEntity} are read while the session is open, also from background work such as the AI triage.
 */
@Component
@Transactional
class JpaTicketStore implements TicketStore, TicketSearch {

    private final TicketJpaRepository repo;
    private final Clock clock;

    JpaTicketStore(TicketJpaRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ TicketStore

    @Override
    public Optional<Ticket> lockForUpdate(UUID id) {
        return repo.findForUpdate(id).map(TicketEntity::toDomain);
    }

    @Override
    public Optional<Ticket> find(UUID id) {
        return repo.findById(id).map(TicketEntity::toDomain);
    }

    @Override
    public void save(Ticket ticket) {
        TicketEntity e = repo.findById(ticket.id()).orElseGet(() -> new TicketEntity(ticket.id()));
        e.copyFrom(ticket);
        repo.saveAndFlush(e);
    }

    @Override
    public long nextNumber() {
        return repo.nextTicketNumber();
    }

    @Override
    public boolean existsByLegacyId(UUID legacyId) {
        return repo.existsByLegacyId(legacyId);
    }

    @Override
    public void updatePriorityRank(String priorityCode, int rank) {
        repo.updatePriorityRank(priorityCode, rank);
    }

    // ------------------------------------------------------------------ TicketSearch

    @Override
    public PageResult<Ticket> search(TicketSearchCriteria criteria, UUID currentUserId, PageQuery page,
                                     TicketSearch.Sort sort) {
        org.springframework.data.domain.Sort.Direction dir = sort.ascending()
                ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        org.springframework.data.domain.Sort s = org.springframework.data.domain.Sort.by(dir, sort.field());
        if (!"createdAt".equals(sort.field())) {
            s = s.and(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,
                    "createdAt"));
        }
        Page<TicketEntity> result = repo.findAll(TicketSpecifications.of(criteria, currentUserId, clock.instant()),
                PageRequest.of(page.page(), page.size(), s));
        return new PageResult<>(result.getContent().stream().map(TicketEntity::toDomain).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<Ticket> list(TicketSearchCriteria criteria, UUID currentUserId, int limit) {
        return repo.findAll(TicketSpecifications.of(criteria, currentUserId, clock.instant()),
                        PageRequest.of(0, Math.max(1, limit), org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(TicketEntity::toDomain).toList();
    }

    @Override
    public List<Ticket> createdBetween(Instant from, Instant to) {
        return repo.findCreatedBetween(from, to).stream().map(TicketEntity::toDomain).toList();
    }

    @Override
    public List<UUID> resolvedBefore(Collection<String> resolvedStatuses, Instant before, int limit) {
        return resolvedStatuses.isEmpty() ? List.of()
                : repo.findResolvedBefore(resolvedStatuses, before, PageRequest.of(0, Math.max(1, limit)));
    }

    @Override
    public long countByRequester(UUID requesterId) {
        return repo.countByRequesterId(requesterId);
    }

    @Override
    public long countWithStatus(Collection<String> statuses) {
        return statuses.isEmpty() ? 0 : repo.countByStatusIn(statuses);
    }

    @Override
    public long countUnassigned(Collection<String> statuses) {
        return statuses.isEmpty() ? 0 : repo.countByAssignedAgentIdIsNullAndStatusIn(statuses);
    }

    @Override
    public long countAssignedTo(UUID agentId, Collection<String> statuses) {
        return statuses.isEmpty() ? 0 : repo.countByAssignedAgentIdAndStatusIn(agentId, statuses);
    }

    @Override
    public long countEscalated(Collection<String> statuses) {
        return statuses.isEmpty() ? 0 : repo.countByEscalatedTrueAndStatusIn(statuses);
    }

    @Override
    public long countResolvedSince(Instant since) {
        return repo.countByResolvedAtGreaterThanEqual(since);
    }

    @Override
    public long countBreached(Collection<String> openStatuses, Instant now) {
        return openStatuses.isEmpty() ? 0 : repo.countBreached(openStatuses, now);
    }

    @Override
    public Map<String, Long> countByPriority(Collection<String> statuses) {
        return toMap(statuses.isEmpty() ? List.of() : repo.countByPriority(statuses));
    }

    @Override
    public Map<String, Long> countByCategory(Collection<String> statuses) {
        return toMap(statuses.isEmpty() ? List.of() : repo.countByCategory(statuses));
    }

    @Override
    public Map<UUID, Long> openLoadByAgent(Collection<String> openStatuses) {
        Map<UUID, Long> load = new HashMap<>();
        if (!openStatuses.isEmpty()) {
            for (Object[] row : repo.openLoadByAgent(openStatuses)) {
                load.put((UUID) row[0], (Long) row[1]);
            }
        }
        return load;
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((String) row[0], (Long) row[1]);
        }
        return map;
    }
}
