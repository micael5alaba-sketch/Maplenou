package com.maplenou.support.ticket.application;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.sla.application.SlaAdminService;
import com.maplenou.support.ticket.application.port.TicketStore;
import com.maplenou.support.ticket.application.port.VocabularyStore;
import com.maplenou.support.ticket.domain.PermissionMatrix;
import com.maplenou.support.ticket.domain.PermissionRule;
import com.maplenou.support.ticket.domain.TicketAccessPolicy;
import com.maplenou.support.ticket.domain.vocabulary.Priorities;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The configurable vocabulary of the ticketing core (DEC-CS11-02): statuses and transitions, priorities, and the
 * permission matrix. Read on every request, so it is cached for {@code REFERENCE_CACHE_TTL} and reloaded after an
 * administrator's change is committed. Every change is validated so the fixed rules keep holding.
 */
@Service
public class VocabularyService {

    /** Everything the clients need to label and filter tickets. */
    public record Vocabulary(List<StatusDefinition> statuses, List<Transition> transitions,
                             List<PriorityDefinition> priorities) {
    }

    private record Snapshot(Workflow workflow, Priorities priorities, PermissionMatrix matrix, Instant loadedAt) {
    }

    private final VocabularyStore store;
    private final TicketStore tickets;
    private final SlaAdminService slaAdmin;
    private final Clock clock;
    private final Duration ttl;
    private volatile Snapshot snapshot;

    public VocabularyService(VocabularyStore store, TicketStore tickets, SlaAdminService slaAdmin, Clock clock,
                             TicketSettings settings) {
        this.store = store;
        this.tickets = tickets;
        this.slaAdmin = slaAdmin;
        this.clock = clock;
        this.ttl = settings.referenceTtl();
    }

    public Workflow workflow() {
        return current().workflow();
    }

    public Priorities priorities() {
        return current().priorities();
    }

    public PermissionMatrix matrix() {
        return current().matrix();
    }

    public TicketAccessPolicy policy() {
        Snapshot s = current();
        return new TicketAccessPolicy(s.matrix(), s.workflow());
    }

    public Vocabulary vocabulary() {
        Snapshot s = current();
        return new Vocabulary(s.workflow().statuses(), s.workflow().transitions(), s.priorities().all());
    }

    // ------------------------------------------------------------------ administration (BR-065: administrators)

    @Transactional
    public Vocabulary replaceWorkflow(Actor admin, List<StatusDefinition> statuses, List<Transition> transitions) {
        requireAdmin(admin);
        Workflow current = workflow();
        Map<String, StatusDefinition> before = new HashMap<>();
        current.statuses().forEach(s -> before.put(s.code(), s));
        List<StatusDefinition> normalized = new ArrayList<>();
        for (StatusDefinition s : statuses) {
            StatusDefinition old = before.get(s.code());
            boolean system = old != null && old.system();
            // system statuses keep their kind; a new or custom status is never "system"
            StatusKind kind = system ? old.kind() : s.kind();
            normalized.add(new StatusDefinition(s.code(), kind, s.labelFr().trim(), blank(s.labelEn()), s.sortOrder(),
                    s.slaPaused(), system, s.active()));
        }
        for (StatusDefinition old : before.values()) {
            boolean kept = normalized.stream().anyMatch(s -> s.code().equals(old.code()));
            if (!kept && (old.system() || store.statusInUse(old.code()))) {
                throw SupportException.businessRule("STATUS_IN_USE",
                        "Le statut " + old.code() + " ne peut pas être supprimé (statut système ou utilisé) : "
                                + "désactivez-le.", "DEC-CS11-02");
            }
        }
        List<String> problems = new Workflow(normalized, transitions).problems();
        if (!problems.isEmpty()) {
            throw SupportException.businessRule("INVALID_WORKFLOW", String.join(" ", problems), "BR-010");
        }
        store.replaceStatuses(normalized, transitions);
        invalidate();
        return vocabularyAfterWrite(normalized, transitions, priorities().all());
    }

    @Transactional
    public Vocabulary replacePriorities(Actor admin, List<PriorityDefinition> priorities) {
        requireAdmin(admin);
        Map<String, PriorityDefinition> before = new HashMap<>();
        priorities().all().forEach(p -> before.put(p.code(), p));
        List<PriorityDefinition> normalized = new ArrayList<>();
        for (PriorityDefinition p : priorities) {
            PriorityDefinition old = before.get(p.code());
            normalized.add(new PriorityDefinition(p.code(), p.rank(), p.labelFr().trim(), blank(p.labelEn()),
                    p.requesterSelectable(), p.defaultPriority(), old != null && old.system(), p.active()));
        }
        for (PriorityDefinition old : before.values()) {
            boolean kept = normalized.stream().anyMatch(p -> p.code().equals(old.code()));
            if (!kept && (old.system() || store.priorityInUse(old.code()))) {
                throw SupportException.businessRule("PRIORITY_IN_USE",
                        "La priorité " + old.code() + " ne peut pas être supprimée (priorité système ou utilisée) : "
                                + "désactivez-la.", "DEC-CS11-02");
            }
        }
        List<String> problems = new Priorities(normalized).problems();
        if (!problems.isEmpty()) {
            throw SupportException.businessRule("INVALID_PRIORITIES", String.join(" ", problems), "BR-007");
        }
        store.replacePriorities(normalized);
        for (PriorityDefinition p : normalized) {
            PriorityDefinition old = before.get(p.code());
            if (old == null) {
                slaAdmin.ensurePolicy(p.code(), priorities().defaultPriority().code());
            } else if (old.rank() != p.rank()) {
                tickets.updatePriorityRank(p.code(), p.rank());
            }
        }
        invalidate();
        Workflow wf = workflow();
        return vocabularyAfterWrite(wf.statuses(), wf.transitions(), normalized);
    }

    @Transactional(readOnly = true)
    public List<PermissionRule> permissionRules(Actor admin) {
        requireAdmin(admin);
        return List.copyOf(matrix().rules());
    }

    @Transactional
    public List<PermissionRule> replacePermissionRules(Actor admin, List<PermissionRule> rules) {
        requireAdmin(admin);
        List<String> problems = PermissionMatrix.problems(rules);
        if (!problems.isEmpty()) {
            throw SupportException.businessRule("INVALID_PERMISSIONS", String.join(" ", problems), "BR-076");
        }
        boolean adminKeepsAdministration = rules.stream()
                .anyMatch(r -> r.role() == Role.ADMINISTRATOR && r.action().name().startsWith("VIEW_"));
        if (!adminKeepsAdministration) {
            throw SupportException.businessRule("INVALID_PERMISSIONS",
                    "Les administrateurs doivent garder la consultation des tickets.", "BR-076");
        }
        store.replacePermissionRules(List.copyOf(Set.copyOf(rules)));
        invalidate();
        return List.copyOf(Set.copyOf(rules));
    }

    /** The matrix delivered with the product (to restore it). */
    public List<PermissionRule> defaultPermissionRules() {
        return List.copyOf(PermissionMatrix.defaults().rules());
    }

    private static Vocabulary vocabularyAfterWrite(List<StatusDefinition> s, List<Transition> t,
                                                   List<PriorityDefinition> p) {
        return new Vocabulary(new Workflow(s, t).statuses(), t, new Priorities(p).all());
    }

    private static void requireAdmin(Actor admin) {
        if (admin.role() != Role.ADMINISTRATOR) {
            throw SupportException.forbidden("Réservé aux administrateurs.", "BR-065");
        }
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ------------------------------------------------------------------ cache

    private Snapshot current() {
        Snapshot s = snapshot;
        if (s == null || s.loadedAt().plus(ttl).isBefore(clock.instant())) {
            s = load();
            snapshot = s;
        }
        return s;
    }

    private Snapshot load() {
        Workflow wf = new Workflow(store.statuses(), store.transitions());
        Priorities pr = new Priorities(store.priorities());
        PermissionMatrix m = new PermissionMatrix(store.permissionRules());
        return new Snapshot(wf, pr, m, clock.instant());
    }

    /** Reload now and again after commit (a request running meanwhile could have re-read the old values). */
    public void invalidate() {
        snapshot = null;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    snapshot = null;
                }
            });
        }
    }
}
