package com.maplenou.support.ticket.domain.vocabulary;

import com.maplenou.support.shared.SupportException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The configured priorities, least urgent first (FR-004, BR-007). */
public final class Priorities {

    private final List<PriorityDefinition> list;

    public Priorities(Collection<PriorityDefinition> priorities) {
        this.list = priorities.stream().sorted(Comparator.comparingInt(PriorityDefinition::rank)).toList();
    }

    public static Priorities defaults() {
        return new Priorities(List.of(
                new PriorityDefinition("LOW", 1, "Basse", "Low", true, false, true, true),
                new PriorityDefinition("MEDIUM", 2, "Moyenne", "Medium", true, true, true, true),
                new PriorityDefinition("HIGH", 3, "Élevée", "High", true, false, true, true),
                new PriorityDefinition("CRITICAL", 4, "Critique", "Critical", false, false, true, true)));
    }

    public List<PriorityDefinition> all() {
        return list;
    }

    public Optional<PriorityDefinition> find(String code) {
        return list.stream().filter(p -> p.code().equals(code)).findFirst();
    }

    public PriorityDefinition priority(String code) {
        return find(code).orElseThrow(() -> new IllegalStateException("Unknown priority " + code));
    }

    public PriorityDefinition requireUsable(String code) {
        return find(code).filter(PriorityDefinition::active).orElseThrow(() ->
                SupportException.badRequest("UNKNOWN_PRIORITY", "Priorité inconnue : " + code + ".", "BR-007"));
    }

    public PriorityDefinition defaultPriority() {
        return list.stream().filter(p -> p.active() && p.defaultPriority()).findFirst()
                .orElseGet(() -> list.stream().filter(PriorityDefinition::active).findFirst()
                        .orElseThrow(() -> new IllegalStateException("No active priority")));
    }

    public List<String> problems() {
        List<String> problems = new ArrayList<>();
        if (list.stream().noneMatch(p -> p.active() && p.defaultPriority())) {
            problems.add("Une priorité active doit être la priorité par défaut.");
        }
        if (list.stream().filter(PriorityDefinition::defaultPriority).count() > 1) {
            problems.add("Une seule priorité peut être la priorité par défaut.");
        }
        if (list.stream().noneMatch(p -> p.active() && p.requesterSelectable())) {
            problems.add("Les demandeurs doivent pouvoir choisir au moins une priorité.");
        }
        if (list.stream().map(PriorityDefinition::rank).distinct().count() != list.size()) {
            problems.add("Deux priorités ne peuvent pas avoir le même rang.");
        }
        for (PriorityDefinition p : list) {
            if (!p.code().matches("[A-Z][A-Z0-9_]{1,19}")) {
                problems.add("Code de priorité invalide : " + p.code() + " (majuscules, chiffres, _).");
            }
        }
        return problems;
    }
}
