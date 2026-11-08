package com.maplenou.support.ticket.domain.vocabulary;

import com.maplenou.support.shared.SupportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Statuses and manual transitions of the ticket lifecycle (BR-010, docs/diagrams/uml/ticket-state.mmd), as
 * configured by administrators. Rules that never change:
 * <ul>
 *   <li>{@code NEW → ASSIGNED} happens only through assignment, never through a status change;</li>
 *   <li>a resolved ticket goes back to work only through an approved reopening (BR-049..BR-051);</li>
 *   <li>a closed ticket never changes again (BR-052).</li>
 * </ul>
 */
public final class Workflow {

    private final Map<String, StatusDefinition> statuses = new LinkedHashMap<>();
    private final Map<String, Set<String>> manual = new LinkedHashMap<>();
    private final Map<StatusKind, String> systemByKind = new EnumMap<>(StatusKind.class);

    public Workflow(Collection<StatusDefinition> statuses, Collection<Transition> transitions) {
        statuses.stream().sorted(Comparator.comparingInt(StatusDefinition::sortOrder))
                .forEach(s -> this.statuses.put(s.code(), s));
        for (StatusDefinition s : this.statuses.values()) {
            if (s.system()) {
                systemByKind.put(s.kind(), s.code());
            }
        }
        for (Transition t : transitions) {
            manual.computeIfAbsent(t.from(), k -> new LinkedHashSet<>()).add(t.to());
        }
    }

    /** The lifecycle delivered with the product (CS-01): same statuses and transitions as V1. */
    public static Workflow defaults() {
        return new Workflow(List.of(
                new StatusDefinition("NEW", StatusKind.NEW, "Nouveau", "New", 1, false, true, true),
                new StatusDefinition("ASSIGNED", StatusKind.ASSIGNED, "Attribué", "Assigned", 2, false, true, true),
                new StatusDefinition("IN_PROGRESS", StatusKind.IN_PROGRESS, "En cours", "In progress", 3, false, true, true),
                new StatusDefinition("WAITING_FOR_CUSTOMER", StatusKind.WAITING_REQUESTER, "En attente du client",
                        "Waiting for customer", 4, true, true, true),
                new StatusDefinition("RESOLVED", StatusKind.RESOLVED, "Résolu", "Resolved", 5, false, true, true),
                new StatusDefinition("CLOSED", StatusKind.CLOSED, "Fermé", "Closed", 6, false, true, true)),
                List.of(new Transition("ASSIGNED", "IN_PROGRESS"), new Transition("ASSIGNED", "WAITING_FOR_CUSTOMER"),
                        new Transition("IN_PROGRESS", "WAITING_FOR_CUSTOMER"), new Transition("IN_PROGRESS", "RESOLVED"),
                        new Transition("WAITING_FOR_CUSTOMER", "IN_PROGRESS"),
                        new Transition("WAITING_FOR_CUSTOMER", "RESOLVED"), new Transition("RESOLVED", "CLOSED")));
    }

    public List<StatusDefinition> statuses() {
        return List.copyOf(statuses.values());
    }

    public List<Transition> transitions() {
        List<Transition> list = new ArrayList<>();
        manual.forEach((from, tos) -> tos.forEach(to -> list.add(new Transition(from, to))));
        return list;
    }

    public Optional<StatusDefinition> find(String code) {
        return Optional.ofNullable(code == null ? null : statuses.get(code));
    }

    /** The definition of a status that must exist (stored on a ticket). */
    public StatusDefinition status(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Unknown ticket status " + code));
    }

    /** A status requested by a caller: unknown or inactive codes are an input error. */
    public StatusDefinition requireUsable(String code) {
        return find(code).filter(StatusDefinition::active).orElseThrow(() ->
                SupportException.badRequest("UNKNOWN_STATUS", "Statut inconnu : " + code + ".", "BR-010"));
    }

    public StatusKind kind(String code) {
        return status(code).kind();
    }

    public boolean isOpen(String code) {
        return status(code).isOpen();
    }

    public boolean pausesSla(String code) {
        return find(code).map(StatusDefinition::slaPaused).orElse(false);
    }

    /** The system status of a kind (the one automatic changes use). */
    public String system(StatusKind kind) {
        String code = systemByKind.get(kind);
        if (code == null) {
            throw new IllegalStateException("No system status of kind " + kind);
        }
        return code;
    }

    public Set<String> openCodes() {
        Set<String> open = new LinkedHashSet<>();
        statuses.values().stream().filter(StatusDefinition::isOpen).forEach(s -> open.add(s.code()));
        return open;
    }

    public Set<String> codesOfKind(StatusKind kind) {
        Set<String> codes = new LinkedHashSet<>();
        statuses.values().stream().filter(s -> s.kind() == kind).forEach(s -> codes.add(s.code()));
        return codes;
    }

    /** Whether support personnel may move a ticket from {@code from} to {@code to} with a status change. */
    public boolean isManualTransitionAllowed(String from, String to) {
        return manual.getOrDefault(from, Set.of()).contains(to) && find(to).map(StatusDefinition::active).orElse(false);
    }

    public Set<String> manualTargets(String from) {
        Set<String> targets = new LinkedHashSet<>();
        for (String to : manual.getOrDefault(from, Set.of())) {
            if (find(to).map(StatusDefinition::active).orElse(false)) {
                targets.add(to);
            }
        }
        return targets;
    }

    /** Statuses an approved reopening may return the ticket to (BR-051): open statuses after NEW. */
    public Set<String> reopenTargets() {
        Set<String> targets = new LinkedHashSet<>();
        statuses.values().stream()
                .filter(s -> s.active() && s.isOpen() && s.kind() != StatusKind.NEW)
                .forEach(s -> targets.add(s.code()));
        return targets;
    }

    /**
     * Problems that would make this lifecycle unusable (empty = valid). Checked before an administrator's change
     * is saved, so the rules above always hold.
     */
    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        for (StatusKind kind : StatusKind.values()) {
            if (kind != StatusKind.ON_HOLD && !systemByKind.containsKey(kind)) {
                problems.add("Il manque le statut système de type " + kind + ".");
            }
        }
        for (StatusDefinition s : statuses.values()) {
            if (!s.code().matches("[A-Z][A-Z0-9_]{1,24}")) {
                problems.add("Code de statut invalide : " + s.code() + " (majuscules, chiffres, _).");
            }
            if (!s.system() && !s.kind().allowedForCustomStatus()) {
                problems.add("Un statut ajouté doit être de type « en cours » ou « en attente d'un tiers » : " + s.code() + ".");
            }
            if (s.system() && !s.active()) {
                problems.add("Le statut système " + s.code() + " doit rester actif.");
            }
        }
        manual.forEach((from, tos) -> {
            StatusDefinition f = statuses.get(from);
            if (f == null) {
                problems.add("Transition depuis un statut inconnu : " + from + ".");
                return;
            }
            for (String to : tos) {
                StatusDefinition t = statuses.get(to);
                if (t == null) {
                    problems.add("Transition vers un statut inconnu : " + to + ".");
                    continue;
                }
                if (f.kind() == StatusKind.NEW) {
                    problems.add("Un ticket nouveau ne change de statut que par son attribution (" + from + " → " + to + ").");
                }
                if (t.kind() == StatusKind.NEW || t.kind() == StatusKind.ASSIGNED) {
                    problems.add("Le statut " + to + " n'est atteint que par la création ou l'attribution.");
                }
                if (f.kind() == StatusKind.CLOSED) {
                    problems.add("Un ticket fermé ne change plus de statut (" + from + " → " + to + ").");
                }
                if (f.kind() == StatusKind.RESOLVED && t.kind() != StatusKind.CLOSED) {
                    problems.add("Un ticket résolu ne revient au travail que par une réouverture (" + from + " → " + to + ").");
                }
            }
        });
        // every open status (after NEW) must be able to reach a resolved status, otherwise tickets get stuck
        for (StatusDefinition s : statuses.values()) {
            if (s.active() && s.isOpen() && s.kind() != StatusKind.NEW && !reachesResolved(s.code())) {
                problems.add("Depuis le statut " + s.code() + ", aucun chemin ne mène à « Résolu ».");
            }
        }
        String resolved = systemByKind.get(StatusKind.RESOLVED);
        String closed = systemByKind.get(StatusKind.CLOSED);
        if (resolved != null && closed != null && !manual.getOrDefault(resolved, Set.of()).contains(closed)) {
            problems.add("La transition Résolu → Fermé est obligatoire.");
        }
        return problems;
    }

    private boolean reachesResolved(String start) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> todo = new ArrayList<>(List.of(start));
        while (!todo.isEmpty()) {
            String code = todo.removeFirst();
            if (!seen.add(code)) {
                continue;
            }
            StatusDefinition s = statuses.get(code);
            if (s != null && s.kind() == StatusKind.RESOLVED) {
                return true;
            }
            for (String to : manual.getOrDefault(code, Set.of())) {
                if (statuses.containsKey(to) && statuses.get(to).active()) {
                    todo.add(to);
                }
            }
        }
        return false;
    }
}
