package com.maplenou.support.ticket.domain;

import com.maplenou.support.ticket.domain.vocabulary.Priorities;
import com.maplenou.support.ticket.domain.vocabulary.PriorityDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import com.maplenou.support.ticket.domain.vocabulary.Workflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** BR-010 with a configurable lifecycle (DEC-CS11-02): the default workflow keeps exactly the V1 rules. */
class WorkflowTest {

    private final Workflow wf = Workflow.defaults();

    @ParameterizedTest
    @CsvSource({
            "ASSIGNED, IN_PROGRESS", "ASSIGNED, WAITING_FOR_CUSTOMER",
            "IN_PROGRESS, WAITING_FOR_CUSTOMER", "IN_PROGRESS, RESOLVED",
            "WAITING_FOR_CUSTOMER, IN_PROGRESS", "WAITING_FOR_CUSTOMER, RESOLVED",
            "RESOLVED, CLOSED"})
    void allowed(String from, String to) {
        assertThat(wf.isManualTransitionAllowed(from, to)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "NEW, CLOSED", "NEW, RESOLVED", "NEW, IN_PROGRESS", "NEW, ASSIGNED",
            "ASSIGNED, CLOSED", "IN_PROGRESS, CLOSED", "IN_PROGRESS, NEW",
            "RESOLVED, IN_PROGRESS", "RESOLVED, WAITING_FOR_CUSTOMER",
            "CLOSED, IN_PROGRESS", "CLOSED, RESOLVED", "CLOSED, NEW"})
    void rejected(String from, String to) {
        assertThat(wf.isManualTransitionAllowed(from, to)).isFalse();
    }

    @Test
    void defaultsAreValidAndKindsDriveTheRules() {
        assertThat(wf.problems()).isEmpty();
        assertThat(wf.manualTargets("CLOSED")).isEmpty();
        assertThat(wf.isOpen("CLOSED")).isFalse();
        assertThat(wf.isOpen("RESOLVED")).isFalse();
        assertThat(wf.isOpen("WAITING_FOR_CUSTOMER")).isTrue();
        assertThat(wf.pausesSla("WAITING_FOR_CUSTOMER")).isTrue();
        assertThat(wf.pausesSla("IN_PROGRESS")).isFalse();
        assertThat(wf.system(StatusKind.IN_PROGRESS)).isEqualTo("IN_PROGRESS");
        assertThat(wf.reopenTargets()).containsExactlyInAnyOrder("ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER");
    }

    @Test
    void anAdministratorMayAddAnOnHoldStatus() {
        List<StatusDefinition> statuses = new ArrayList<>(wf.statuses());
        statuses.add(new StatusDefinition("ON_HOLD", StatusKind.ON_HOLD, "En attente d'un tiers", "On hold", 4,
                true, false, true));
        List<Transition> transitions = new ArrayList<>(wf.transitions());
        transitions.add(new Transition("IN_PROGRESS", "ON_HOLD"));
        transitions.add(new Transition("ON_HOLD", "IN_PROGRESS"));
        Workflow extended = new Workflow(statuses, transitions);
        assertThat(extended.problems()).isEmpty();
        assertThat(extended.reopenTargets()).contains("ON_HOLD");
        assertThat(extended.pausesSla("ON_HOLD")).isTrue();
    }

    @Test
    void brokenLifecyclesAreRefused() {
        List<StatusDefinition> statuses = new ArrayList<>(wf.statuses());
        statuses.add(new StatusDefinition("STUCK", StatusKind.ON_HOLD, "Bloqué", null, 7, true, false, true));
        List<Transition> transitions = new ArrayList<>(wf.transitions());
        transitions.add(new Transition("IN_PROGRESS", "STUCK"));             // no way out of STUCK
        transitions.add(new Transition("NEW", "IN_PROGRESS"));               // NEW changes only by assignment
        transitions.add(new Transition("RESOLVED", "IN_PROGRESS"));          // reopening only
        transitions.add(new Transition("CLOSED", "IN_PROGRESS"));            // closed is final
        assertThat(new Workflow(statuses, transitions).problems()).hasSize(4);

        List<StatusDefinition> badKind = new ArrayList<>(wf.statuses());
        badKind.add(new StatusDefinition("DONE_TOO", StatusKind.RESOLVED, "Terminé", null, 8, false, false, true));
        assertThat(new Workflow(badKind, wf.transitions()).problems()).anyMatch(p -> p.contains("DONE_TOO"));

        List<Transition> noClose = new ArrayList<>(wf.transitions());
        noClose.remove(new Transition("RESOLVED", "CLOSED"));
        assertThat(new Workflow(wf.statuses(), noClose).problems()).anyMatch(p -> p.contains("Fermé"));
    }

    @Test
    void prioritiesNeedOneDefaultAndOneSelectable() {
        assertThat(Priorities.defaults().problems()).isEmpty();
        assertThat(Priorities.defaults().defaultPriority().code()).isEqualTo("MEDIUM");
        assertThat(Priorities.defaults().priority("CRITICAL").requesterSelectable()).isFalse();
        assertThat(new Priorities(List.of(new PriorityDefinition("ONLY", 1, "Seule", null, false, false, false, true)))
                .problems()).hasSize(2);
    }

    @Test
    void ticketNumberFormat() {
        assertThat(TicketNumbers.format(2026, 125)).isEqualTo("TKT-2026-00125");
        assertThat(TicketNumbers.format(2026, 123456)).isEqualTo("TKT-2026-123456");
    }
}
