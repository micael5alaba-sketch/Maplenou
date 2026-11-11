package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.PermissionRule;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.Transition;

import java.util.List;

/**
 * Configurable vocabulary (tables {@code ticket_statuses}, {@code ticket_transitions}, {@code ticket_priorities},
 * {@code permission_rules}). Each {@code replace…} call replaces the whole set in the current transaction.
 */
public interface VocabularyStore {

    List<StatusDefinition> statuses();

    List<Transition> transitions();

    List<PriorityDefinition> priorities();

    List<PermissionRule> permissionRules();

    void replaceStatuses(List<StatusDefinition> statuses, List<Transition> transitions);

    void replacePriorities(List<PriorityDefinition> priorities);

    void replacePermissionRules(List<PermissionRule> rules);

    /** Tickets currently using a status or a priority (it may then only be deactivated). */
    boolean statusInUse(String code);

    boolean priorityInUse(String code);
}
