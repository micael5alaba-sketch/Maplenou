package com.maplenou.support.ticket.domain;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.domain.TicketAccessPolicy.TicketFacts;
import com.maplenou.support.ticket.domain.vocabulary.Priorities;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.maplenou.support.ticket.domain.TicketAction.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Authorization matrix and BR-056..BR-076, checked at the policy level with the default matrix. */
class TicketAccessPolicyTest {

    private final Workflow wf = Workflow.defaults();
    private final TicketAccessPolicy policy = new TicketAccessPolicy(PermissionMatrix.defaults(), wf);
    private final Instant now = Instant.parse("2026-10-08T10:00:00Z");

    private final Actor customer = user(Role.CUSTOMER);
    private final Actor otherCustomer = user(Role.CUSTOMER);
    private final Actor agent = user(Role.SUPPORT_AGENT);
    private final Actor otherAgent = user(Role.SUPPORT_AGENT);
    private final Actor manager = user(Role.SUPPORT_MANAGER);
    private final Actor admin = user(Role.ADMINISTRATOR);

    private static Actor user(Role role) {
        return new Actor(UUID.randomUUID(), role, role.name(), null, null);
    }

    private Ticket ticket(String status, Actor assignee) {
        Ticket t = Ticket.open(UUID.randomUUID(), "TKT-2026-00001", "s", "d", "ORDER",
                Priorities.defaults().priority("MEDIUM"), "NEW", customer.id(), Role.CUSTOMER, false,
                ContextRefs.NONE, now);
        if (assignee != null) {
            t.assignTo(assignee.id(), now);
        }
        if (!"NEW".equals(status)) {
            t.changeStatus(status, wf.kind(status), now);
        }
        return t;
    }

    private static TicketFacts facts(boolean pendingReopen) {
        return new TicketFacts(pendingReopen, false);
    }

    @Test
    void requesterSeesOnlyOwnTicket() {
        Ticket t = ticket("NEW", null);
        assertThat(policy.canView(customer, t)).isTrue();
        assertThat(policy.canView(otherCustomer, t)).isFalse();
        assertThatThrownBy(() -> policy.requireView(otherCustomer, t))
                .isInstanceOf(SupportException.class).extracting("code").isEqualTo("NOT_FOUND");
    }

    @Test
    void requesterCanMessageReopenAndRate() {
        assertThat(policy.allowedActions(customer, ticket("IN_PROGRESS", agent), facts(false)))
                .containsExactly(SEND_MESSAGE);
        assertThat(policy.allowedActions(customer, ticket("RESOLVED", agent), facts(false)))
                .containsExactlyInAnyOrder(SEND_MESSAGE, REQUEST_REOPEN, RATE);
        assertThat(policy.allowedActions(customer, ticket("RESOLVED", agent), facts(true)))
                .containsExactlyInAnyOrder(SEND_MESSAGE, RATE);
        assertThat(policy.allowedActions(customer, ticket("CLOSED", agent), facts(false))).containsExactly(RATE);
        assertThat(policy.allowedActions(customer, ticket("CLOSED", agent), new TicketFacts(false, true))).isEmpty();
    }

    @Test
    void requesterNeverGetsSupportActions() {
        var actions = policy.allowedActions(customer, ticket("IN_PROGRESS", agent), facts(false));
        assertThat(actions).doesNotContain(ADD_INTERNAL_NOTE, VIEW_INTERNAL, SET_CRITICAL, CHANGE_PRIORITY,
                CHANGE_STATUS, RESOLVE, CLOSE, VIEW_CONTEXT, VIEW_SLA, USE_AI_ASSIST, REQUEST_REFUND);
    }

    @Test
    void agentHasFullAuthorityOnOwnTicket() {
        assertThat(policy.allowedActions(agent, ticket("IN_PROGRESS", agent), facts(false)))
                .contains(SEND_MESSAGE, ADD_INTERNAL_NOTE, CHANGE_STATUS, RESOLVE, CHANGE_PRIORITY, SET_CRITICAL,
                        CHANGE_CATEGORY, ESCALATE, USE_AI_ASSIST, REQUEST_REFUND, RESCHEDULE_DELIVERY, CONTACT_DRIVER)
                .doesNotContain(REASSIGN, ASSIGN_SELF, RATE);
        assertThat(policy.allowedActions(agent, ticket("RESOLVED", agent), facts(false))).contains(CLOSE);
        assertThat(policy.allowedActions(agent, ticket("RESOLVED", agent), facts(true)))
                .contains(DECIDE_REOPEN).doesNotContain(CLOSE);
    }

    @Test
    void agentCanTriageAndSelfAssignUnassignedTicket() {
        assertThat(policy.allowedActions(agent, ticket("NEW", null), facts(false)))
                .contains(ASSIGN_SELF, CHANGE_PRIORITY, CHANGE_CATEGORY, ADD_INTERNAL_NOTE, ESCALATE, USE_AI_ASSIST)
                .doesNotContain(SEND_MESSAGE, CHANGE_STATUS, RESOLVE, REASSIGN, REQUEST_REFUND);
    }

    @Test
    void agentCanOnlyViewAnotherAgentsTicket() {
        var actions = policy.allowedActions(agent, ticket("IN_PROGRESS", otherAgent), facts(false));
        assertThat(policy.canView(agent, ticket("IN_PROGRESS", otherAgent))).isTrue();
        assertThat(actions).containsExactlyInAnyOrder(VIEW_INTERNAL, VIEW_CONTEXT, VIEW_SLA);
    }

    @Test
    void managerAndAdminCanReassignAnyTicket() {
        assertThat(policy.allowedActions(manager, ticket("IN_PROGRESS", agent), facts(false)))
                .contains(REASSIGN, ASSIGN_SELF, SEND_MESSAGE, RESOLVE);
        assertThat(policy.allowedActions(admin, ticket("IN_PROGRESS", agent), facts(false)))
                .contains(REASSIGN, RESOLVE).doesNotContain(ASSIGN_SELF); // admins are not assignable
    }

    @Test
    void closedTicketsAreReadOnlyForSupport() {
        assertThat(policy.allowedActions(manager, ticket("CLOSED", agent), facts(false)))
                .containsExactlyInAnyOrder(VIEW_INTERNAL, VIEW_CONTEXT, VIEW_SLA);
    }

    @Test
    void requireThrowsForbiddenWithRule() {
        Ticket t = ticket("IN_PROGRESS", otherAgent);
        assertThatThrownBy(() -> policy.require(agent, t, facts(false), REASSIGN))
                .isInstanceOf(SupportException.class).extracting("rule").isEqualTo("BR-058");
    }

    @Test
    void theMatrixIsData() {
        // remove "agents may resolve their tickets": they keep everything else
        var rules = PermissionMatrix.defaults().rules().stream()
                .filter(r -> !(r.role() == Role.SUPPORT_AGENT && r.action() == RESOLVE)).toList();
        TicketAccessPolicy strict = new TicketAccessPolicy(new PermissionMatrix(rules), wf);
        assertThat(strict.allowedActions(agent, ticket("IN_PROGRESS", agent), facts(false)))
                .contains(SEND_MESSAGE, CHANGE_STATUS).doesNotContain(RESOLVE);
    }

    @Test
    void unsafeMatricesAreRefused() {
        assertThat(PermissionMatrix.problems(PermissionMatrix.defaults().rules())).isEmpty();
        assertThat(PermissionMatrix.problems(List.of(
                new PermissionRule(Role.CUSTOMER, VIEW_INTERNAL, PermissionScope.OWN),       // support-only action
                new PermissionRule(Role.CUSTOMER, SEND_MESSAGE, PermissionScope.ANY),        // other people's tickets
                new PermissionRule(Role.SUPPORT_AGENT, RATE, PermissionScope.ANY),           // requester action
                new PermissionRule(Role.ADMINISTRATOR, ASSIGN_SELF, PermissionScope.ANY))))  // not assignable
                .hasSize(5);
    }
}
