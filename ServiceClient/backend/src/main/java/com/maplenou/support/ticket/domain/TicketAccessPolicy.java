package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;

import java.util.EnumSet;
import java.util.Set;

import static com.maplenou.support.ticket.domain.TicketAction.*;

/**
 * Resource-level authorization for tickets (FR-012, BR-056..BR-076). Two questions, answered separately:
 * <ol>
 *   <li><b>may this person do this kind of thing on this ticket?</b> — the {@link PermissionMatrix} (configurable),
 *       from the caller's role and relationship to the ticket (requester, assignee, unassigned ticket);</li>
 *   <li><b>does the ticket's state allow it now?</b> — fixed rules below (a closed ticket is read-only, a reopening
 *       is decided only while pending, the requester rates only a resolved ticket once…).</li>
 * </ol>
 * Visibility is not configurable: requesters see only their own tickets (BR-015), support sees all (BR-067),
 * and viewing grants nothing else (BR-070). Anything not allowed is denied (BR-076).
 */
public final class TicketAccessPolicy {

    /** What the policy needs to know about a ticket besides the aggregate. */
    public record TicketFacts(boolean pendingReopen, boolean rated) {
        public static final TicketFacts NONE = new TicketFacts(false, false);
    }

    private final PermissionMatrix matrix;
    private final Workflow workflow;

    public TicketAccessPolicy(PermissionMatrix matrix, Workflow workflow) {
        this.matrix = matrix;
        this.workflow = workflow;
    }

    public boolean canView(Actor user, Ticket ticket) {
        return user.role() != null && (user.role().isSupportStaff() || ticket.requesterId().equals(user.id()));
    }

    /** Throws 404 rather than 403 to requesters so ticket existence is not disclosed (AC-SB-017). */
    public void requireView(Actor user, Ticket ticket) {
        if (!canView(user, ticket)) {
            throw SupportException.notFound("Ticket");
        }
    }

    /** The caller's relationships to the ticket, as permission scopes. */
    public static Set<PermissionScope> relationship(Actor user, Ticket ticket) {
        Set<PermissionScope> scopes = EnumSet.of(PermissionScope.ANY);
        if (ticket.requesterId().equals(user.id())) {
            scopes.add(PermissionScope.OWN);
        }
        if (ticket.isAssignedTo(user.id())) {
            scopes.add(PermissionScope.ASSIGNED);
        }
        if (ticket.assignedAgentId() == null) {
            scopes.add(PermissionScope.UNASSIGNED);
        }
        return scopes;
    }

    /** Manager/administrator, or the agent the ticket is assigned to (BR-056, BR-059): may change its status. */
    public boolean hasOperationalAuthority(Actor user, Ticket ticket) {
        return user.role() != null && user.role().isSupportStaff()
                && matrix.allows(user.role(), CHANGE_STATUS, staffScopes(user, ticket));
    }

    public Set<TicketAction> allowedActions(Actor user, Ticket ticket, TicketFacts facts) {
        Set<TicketAction> actions = EnumSet.noneOf(TicketAction.class);
        if (!canView(user, ticket)) {
            return actions;
        }
        Role role = user.role();
        StatusKind kind = workflow.kind(ticket.statusCode());
        boolean closed = kind == StatusKind.CLOSED;
        boolean owner = ticket.requesterId().equals(user.id());

        // requester side: only on their own tickets
        if (owner) {
            Set<PermissionScope> own = EnumSet.of(PermissionScope.OWN);
            if (!closed && matrix.allows(role, SEND_MESSAGE, own)) {
                actions.add(SEND_MESSAGE);
            }
            if (kind == StatusKind.RESOLVED && !facts.pendingReopen() && matrix.allows(role, REQUEST_REOPEN, own)) {
                actions.add(REQUEST_REOPEN);
            }
            if ((kind == StatusKind.RESOLVED || closed) && !facts.rated() && matrix.allows(role, RATE, own)) {
                actions.add(RATE);
            }
        }
        if (!role.isSupportStaff()) {
            return actions;
        }

        Set<PermissionScope> scopes = staffScopes(user, ticket);
        for (TicketAction view : new TicketAction[]{VIEW_INTERNAL, VIEW_CONTEXT, VIEW_SLA}) {
            if (matrix.allows(role, view, scopes)) {
                actions.add(view);
            }
        }
        if (closed) {
            return actions;
        }
        for (TicketAction a : new TicketAction[]{CHANGE_PRIORITY, SET_CRITICAL, CHANGE_CATEGORY, ADD_INTERNAL_NOTE,
                USE_AI_ASSIST, REQUEST_REFUND, RESCHEDULE_DELIVERY, CONTACT_DRIVER, REASSIGN}) {
            if (matrix.allows(role, a, scopes)) {
                actions.add(a);
            }
        }
        if (!ticket.isEscalated() && matrix.allows(role, ESCALATE, scopes)) {
            actions.add(ESCALATE);
        }
        if (role.isAssignable() && !ticket.isAssignedTo(user.id()) && matrix.allows(role, ASSIGN_SELF, scopes)) {
            actions.add(ASSIGN_SELF);
        }
        if (matrix.allows(role, SEND_MESSAGE, scopes)) {
            actions.add(SEND_MESSAGE);
        }
        String status = ticket.statusCode();
        boolean anyChange = workflow.manualTargets(status).stream()
                .anyMatch(t -> workflow.kind(t) != StatusKind.RESOLVED && workflow.kind(t) != StatusKind.CLOSED);
        if (anyChange && matrix.allows(role, CHANGE_STATUS, scopes)) {
            actions.add(CHANGE_STATUS);
        }
        boolean canResolve = workflow.manualTargets(status).stream().anyMatch(t -> workflow.kind(t) == StatusKind.RESOLVED);
        if (canResolve && matrix.allows(role, RESOLVE, scopes)) {
            actions.add(RESOLVE);
        }
        boolean canClose = workflow.manualTargets(status).stream().anyMatch(t -> workflow.kind(t) == StatusKind.CLOSED);
        if (kind == StatusKind.RESOLVED && canClose && !facts.pendingReopen() && matrix.allows(role, CLOSE, scopes)) {
            actions.add(CLOSE);
        }
        if (facts.pendingReopen() && matrix.allows(role, DECIDE_REOPEN, scopes)) {
            actions.add(DECIDE_REOPEN);
        }
        return actions;
    }

    /** The action a status change needs: resolving and closing are separate permissions. */
    public TicketAction actionFor(String targetStatus) {
        return switch (workflow.kind(targetStatus)) {
            case RESOLVED -> RESOLVE;
            case CLOSED -> CLOSE;
            default -> CHANGE_STATUS;
        };
    }

    public void require(Actor user, Ticket ticket, TicketFacts facts, TicketAction action) {
        requireView(user, ticket);
        if (!allowedActions(user, ticket, facts).contains(action)) {
            throw SupportException.forbidden(denialMessage(action), ruleFor(action));
        }
    }

    /** Support staff never act through the OWN scope; it only matters for requesters. */
    private static Set<PermissionScope> staffScopes(Actor user, Ticket ticket) {
        Set<PermissionScope> scopes = relationship(user, ticket);
        scopes.remove(PermissionScope.OWN);
        return scopes;
    }

    private static String denialMessage(TicketAction action) {
        return switch (action) {
            case REQUEST_REOPEN -> "La réouverture ne peut être demandée que pour votre propre ticket résolu.";
            case REASSIGN -> "Seul un responsable support peut réattribuer un ticket.";
            case ADD_INTERNAL_NOTE, VIEW_INTERNAL -> "Les notes internes sont réservées au support autorisé.";
            case RATE -> "Seul le demandeur peut noter un ticket résolu, une seule fois.";
            default -> "Vous n'êtes pas autorisé à effectuer cette action sur ce ticket.";
        };
    }

    private static String ruleFor(TicketAction action) {
        return switch (action) {
            case REQUEST_REOPEN -> "BR-048";
            case DECIDE_REOPEN -> "BR-050";
            case REASSIGN -> "BR-058";
            case ASSIGN_SELF -> "BR-057";
            case ADD_INTERNAL_NOTE, VIEW_INTERNAL -> "BR-016";
            case SET_CRITICAL -> "BR-036";
            case RATE -> "DEC-CS11-05";
            default -> "BR-071";
        };
    }
}
