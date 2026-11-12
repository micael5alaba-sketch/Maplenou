package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Role;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.maplenou.support.shared.Role.*;
import static com.maplenou.support.ticket.domain.PermissionScope.*;
import static com.maplenou.support.ticket.domain.TicketAction.*;

/**
 * The authorization matrix (docs/requirements/authorization-matrix.md) as data, editable by administrators.
 * Who may <em>see</em> a ticket is not configurable (requesters: their own; support: all — BR-015, BR-067).
 */
public final class PermissionMatrix {

    private final Set<PermissionRule> rules;
    /** role → action → scopes, built once: {@link #allows} is O(1) instead of a scan of every rule. */
    private final Map<Role, Map<TicketAction, Set<PermissionScope>>> index = new EnumMap<>(Role.class);

    public PermissionMatrix(Collection<PermissionRule> rules) {
        this.rules = Set.copyOf(rules);
        for (PermissionRule r : this.rules) {
            index.computeIfAbsent(r.role(), k -> new EnumMap<>(TicketAction.class))
                    .computeIfAbsent(r.action(), k -> EnumSet.noneOf(PermissionScope.class))
                    .add(r.scope());
        }
    }

    /** CS-01 matrix (BR-056..BR-076, DEC-CS03-05), extended with the actions of iteration 6. */
    public static PermissionMatrix defaults() {
        List<PermissionRule> r = new ArrayList<>();
        for (Role requester : List.of(CUSTOMER, SELLER, DELIVERY_DRIVER, ADMINISTRATOR)) {
            for (TicketAction a : List.of(SEND_MESSAGE, REQUEST_REOPEN, RATE)) {
                r.add(new PermissionRule(requester, a, OWN));
            }
        }
        for (Role staff : List.of(SUPPORT_AGENT, SUPPORT_MANAGER, ADMINISTRATOR)) {
            for (TicketAction a : List.of(VIEW_INTERNAL, VIEW_CONTEXT, VIEW_SLA)) {
                r.add(new PermissionRule(staff, a, ANY));
            }
        }
        List<TicketAction> triage = List.of(CHANGE_PRIORITY, SET_CRITICAL, CHANGE_CATEGORY, ADD_INTERNAL_NOTE, ESCALATE,
                USE_AI_ASSIST);
        List<TicketAction> operational = List.of(SEND_MESSAGE, CHANGE_STATUS, RESOLVE, CLOSE, DECIDE_REOPEN,
                REQUEST_REFUND, RESCHEDULE_DELIVERY, CONTACT_DRIVER);
        for (TicketAction a : triage) {
            r.add(new PermissionRule(SUPPORT_AGENT, a, ASSIGNED));
            r.add(new PermissionRule(SUPPORT_AGENT, a, UNASSIGNED));
            r.add(new PermissionRule(SUPPORT_MANAGER, a, ANY));
            r.add(new PermissionRule(ADMINISTRATOR, a, ANY));
        }
        for (TicketAction a : operational) {
            r.add(new PermissionRule(SUPPORT_AGENT, a, ASSIGNED));
            r.add(new PermissionRule(SUPPORT_MANAGER, a, ANY));
            r.add(new PermissionRule(ADMINISTRATOR, a, ANY));
        }
        r.add(new PermissionRule(SUPPORT_AGENT, ASSIGN_SELF, UNASSIGNED));
        r.add(new PermissionRule(SUPPORT_MANAGER, ASSIGN_SELF, ANY));
        r.add(new PermissionRule(SUPPORT_MANAGER, REASSIGN, ANY));
        r.add(new PermissionRule(ADMINISTRATOR, REASSIGN, ANY));
        return new PermissionMatrix(r);
    }

    public Set<PermissionRule> rules() {
        return rules;
    }

    /** Whether one of the caller's relationships to the ticket is covered for this action. */
    public boolean allows(Role role, TicketAction action, Set<PermissionScope> relationship) {
        Map<TicketAction, Set<PermissionScope>> byAction = role == null ? null : index.get(role);
        Set<PermissionScope> scopes = byAction == null ? null : byAction.get(action);
        if (scopes == null) {
            return false;
        }
        for (PermissionScope scope : relationship) { // at most 4 scopes
            if (scopes.contains(scope)) {
                return true;
            }
        }
        return false;
    }

    public Set<TicketAction> actionsOf(Role role) {
        Map<TicketAction, Set<PermissionScope>> byAction = role == null ? null : index.get(role);
        return byAction == null ? EnumSet.noneOf(TicketAction.class) : EnumSet.copyOf(byAction.keySet());
    }

    /**
     * Rules that would break a safety rule (empty = valid): requesters never get support actions, support actions
     * are never given on the caller's own requests only, requester actions are only given on {@code OWN} tickets.
     */
    public static List<String> problems(Collection<PermissionRule> candidate) {
        List<String> problems = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (PermissionRule r : candidate) {
            String label = r.role() + " / " + r.action() + " / " + r.scope();
            if (!seen.add(label)) {
                continue;
            }
            TicketAction.Audience audience = r.action().audience();
            if (audience == TicketAction.Audience.STAFF && !r.role().isSupportStaff()) {
                problems.add("Action réservée au support donnée à un demandeur : " + label + ".");
            }
            if (audience == TicketAction.Audience.REQUESTER && r.scope() != OWN) {
                problems.add("Action de demandeur possible seulement sur ses propres tickets : " + label + ".");
            }
            if (audience == TicketAction.Audience.STAFF && r.scope() == OWN) {
                problems.add("Une action du support ne se limite pas aux tickets du demandeur : " + label + ".");
            }
            if (r.action() == ASSIGN_SELF && !r.role().isAssignable()) {
                problems.add("Seuls les agents et responsables peuvent recevoir des tickets : " + label + ".");
            }
            if (!r.role().isSupportStaff() && r.scope() != OWN) {
                problems.add("Un demandeur n'agit que sur ses propres tickets : " + label + ".");
            }
        }
        return problems;
    }
}
