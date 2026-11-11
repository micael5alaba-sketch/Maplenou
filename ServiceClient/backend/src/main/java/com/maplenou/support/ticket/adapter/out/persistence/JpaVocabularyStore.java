package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.CategoryStore;
import com.maplenou.support.ticket.application.port.VocabularyStore;
import com.maplenou.support.ticket.domain.Category;
import com.maplenou.support.ticket.domain.PermissionRule;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class JpaVocabularyStore implements VocabularyStore, CategoryStore {

    private final StatusJpaRepository statuses;
    private final TransitionJpaRepository transitions;
    private final PriorityJpaRepository priorities;
    private final PermissionRuleJpaRepository rules;
    private final CategoryJpaRepository categories;
    private final TicketJpaRepository tickets;
    private final EntityManager em;

    JpaVocabularyStore(StatusJpaRepository statuses, TransitionJpaRepository transitions,
                       PriorityJpaRepository priorities, PermissionRuleJpaRepository rules,
                       CategoryJpaRepository categories, TicketJpaRepository tickets, EntityManager em) {
        this.statuses = statuses;
        this.transitions = transitions;
        this.priorities = priorities;
        this.rules = rules;
        this.categories = categories;
        this.tickets = tickets;
        this.em = em;
    }

    @Override
    public List<StatusDefinition> statuses() {
        return statuses.findAll().stream().map(StatusEntity::toDomain).toList();
    }

    @Override
    public List<Transition> transitions() {
        return transitions.findAll().stream().map(TransitionEntity::toDomain).toList();
    }

    @Override
    public List<PriorityDefinition> priorities() {
        return priorities.findAll().stream().map(PriorityEntity::toDomain).toList();
    }

    @Override
    public List<PermissionRule> permissionRules() {
        return rules.findAll().stream().map(PermissionRuleEntity::toDomain).toList();
    }

    @Override
    public void replaceStatuses(List<StatusDefinition> list, List<Transition> newTransitions) {
        transitions.deleteAllInBatch();
        Map<String, StatusEntity> existing = statuses.findAll().stream()
                .collect(Collectors.toMap(s -> s.code, Function.identity()));
        Set<String> wanted = list.stream().map(StatusDefinition::code).collect(Collectors.toSet());
        existing.values().stream().filter(e -> !wanted.contains(e.code)).forEach(statuses::delete);
        em.flush();
        for (StatusDefinition s : list) {
            StatusEntity e = existing.get(s.code());
            if (e == null) {
                statuses.save(new StatusEntity(s));
            } else {
                e.copyFrom(s);
            }
        }
        em.flush();
        newTransitions.stream().distinct().forEach(t -> transitions.save(new TransitionEntity(t)));
    }

    @Override
    public void replacePriorities(List<PriorityDefinition> list) {
        Map<String, PriorityEntity> existing = priorities.findAll().stream()
                .collect(Collectors.toMap(p -> p.code, Function.identity()));
        Set<String> wanted = list.stream().map(PriorityDefinition::code).collect(Collectors.toSet());
        existing.values().stream().filter(e -> !wanted.contains(e.code)).forEach(priorities::delete);
        // ranks and the default flag are unique: clear them first, then write the new values
        short temporary = -1;
        for (PriorityEntity e : existing.values()) {
            if (wanted.contains(e.code)) {
                e.rank = temporary--;
                e.defaultPriority = false;
            }
        }
        em.flush();
        for (PriorityDefinition p : list) {
            PriorityEntity e = existing.get(p.code());
            if (e == null) {
                priorities.save(new PriorityEntity(p));
            } else {
                e.copyFrom(p);
            }
        }
        em.flush();
    }

    @Override
    public void replacePermissionRules(List<PermissionRule> list) {
        rules.deleteAllInBatch();
        em.flush();
        list.forEach(r -> rules.save(new PermissionRuleEntity(r)));
    }

    @Override
    public boolean statusInUse(String code) {
        return tickets.existsByStatus(code);
    }

    @Override
    public boolean priorityInUse(String code) {
        return tickets.existsByPriority(code);
    }

    // ------------------------------------------------------------------ categories

    @Override
    public List<Category> all() {
        return categories.findAll().stream().map(CategoryEntity::toDomain).toList();
    }

    @Override
    public Optional<Category> find(String code) {
        return categories.findByCode(code).map(CategoryEntity::toDomain);
    }

    @Override
    public void save(Category category) {
        CategoryEntity e = categories.findByCode(category.code())
                .orElseGet(() -> new CategoryEntity(category.id(), category.code()));
        e.copyFrom(category);
        categories.save(e);
    }
}
