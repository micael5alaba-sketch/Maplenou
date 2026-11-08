package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-005, FR-006, FR-015, FR-016 — AC-004, AC-005, AC-006, AC-010, AC-012, AC-016, AC-022. */
class LifecycleIT extends IntegrationTest {

    @Test
    void ac022_agentTakesUnassignedTicket() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        putJson(agent, "/api/v1/support/tickets/{id}/assign", "{}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAgent.id").value(agent.id().toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    void ac006_managerAssignsToActiveAgentAndItIsAudited() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor agent2 = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        putJson(manager, "/api/v1/support/tickets/{id}/assign", "{\"agentId\":\"" + agent.id() + "\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedAgent.id").value(agent.id().toString()));
        putJson(manager, "/api/v1/support/tickets/{id}/assign", "{\"agentId\":\"" + agent2.id() + "\"}", id)
                .andExpect(status().isOk());
        List<String> actions = json(getAs(manager, "/api/v1/support/tickets/{id}/history", id), "$[*].action");
        assertThat(actions).contains("ASSIGNED", "REASSIGNED");
        List<String> actors = json(getAs(manager, "/api/v1/support/tickets/{id}/history", id),
                "$[?(@.action=='REASSIGNED')].actor.id");
        assertThat(actors).containsExactly(manager.id().toString());
    }

    @Test
    void assignmentTargetMustBeAnActiveSupportAgent() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        String id = createTicket(customer);
        putJson(manager, "/api/v1/support/tickets/{id}/assign", "{\"agentId\":\"" + customer.id() + "\"}", id)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.rule").value("BR-013"));
    }

    @Test
    void agentCannotAssignToSomeoneElse() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor agent2 = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        putJson(agent, "/api/v1/support/tickets/{id}/assign", "{\"agentId\":\"" + agent2.id() + "\"}", id)
                .andExpect(status().isForbidden());
    }

    @Test
    void ac004_validTransitionSucceedsAndIsAudited() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        putJson(agent, "/api/v1/support/tickets/{id}/status", "{\"status\":\"IN_PROGRESS\",\"comment\":\"Investigation\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        // AC-010: actor, action and timestamp recorded
        var history = getAs(agent, "/api/v1/support/tickets/{id}/history", id);
        List<String> newValues = json(history, "$[?(@.action=='STATUS_CHANGED')].newValue");
        assertThat(newValues).contains("ASSIGNED", "IN_PROGRESS");
        List<String> actor = json(history, "$[?(@.newValue=='IN_PROGRESS')].actor.id");
        assertThat(actor).containsExactly(agent.id().toString());
        List<String> at = json(history, "$[?(@.newValue=='IN_PROGRESS')].createdAt");
        assertThat(Instant.parse(at.get(0))).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    void ac005_invalidTransitionIsRejectedAndStatusUnchanged() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        String id = createTicket(customer);
        putJson(manager, "/api/v1/support/tickets/{id}/status", "{\"status\":\"CLOSED\"}", id)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"))
                .andExpect(jsonPath("$.rule").value("BR-010"));
        getAs(manager, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void firstSupportReplyMarksFirstResponseAndStartsWork() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Bonjour, nous regardons.");
        getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sla.firstResponse.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.sla.firstResponse.state").value("MET"));
    }

    @Test
    void requesterReplyWhileWaitingResumesWork() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Pouvez-vous envoyer une photo ?");
        setStatus(agent, id, "WAITING_FOR_CUSTOMER");
        reply(customer, id, "Voici les informations.");
        getAs(customer, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void ac012_closureOfResolvedTicketIsAudited() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = resolvedTicket(customer, agent);
        getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty())
                .andExpect(jsonPath("$.sla.resolution.state").value("MET"));
        setStatus(agent, id, "CLOSED");
        getAs(customer, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                // a closed ticket can still be rated once (CSAT, DEC-CS11-05) and nothing else
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.contains("RATE")));
        postJson(customer, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":5,\"comment\":\"Merci\"}", id)
                .andExpect(status().is2xxSuccessful());
        getAs(customer, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.satisfaction.rating").value(5))
                .andExpect(jsonPath("$.allowedActions").isEmpty());
        // nothing can be posted on a closed ticket
        postJson(customer, "/api/v1/support/tickets/{id}/messages", "{\"body\":\"encore\"}", id)
                .andExpect(status().isForbidden());
    }

    @Test
    void ac016_criticalPriorityBySupportIsAuditedAndRecalculatesSla() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer, ticketJson("PAYMENT", "LOW"));
        assignSelf(agent, id);
        String before = json(getAs(agent, "/api/v1/support/tickets/{id}", id), "$.sla.resolution.dueAt");
        putJson(agent, "/api/v1/support/tickets/{id}/priority", "{\"priority\":\"CRITICAL\",\"reason\":\"Fraude présumée\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));
        String after = json(getAs(agent, "/api/v1/support/tickets/{id}", id), "$.sla.resolution.dueAt");
        assertThat(Instant.parse(after)).isBefore(Instant.parse(before));
        var history = getAs(agent, "/api/v1/support/tickets/{id}/history", id);
        List<String> priorityChange = json(history, "$[?(@.action=='PRIORITY_CHANGED')].newValue");
        assertThat(priorityChange).containsExactly("CRITICAL");
        List<String> recalculated = json(history, "$[?(@.action=='SLA_RECALCULATED')].id");
        assertThat(recalculated).hasSize(1);
    }

    @Test
    void ac015_requesterCannotChangePriority() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        putJson(customer, "/api/v1/support/tickets/{id}/priority", "{\"priority\":\"CRITICAL\"}", id)
                .andExpect(status().isForbidden());
    }

    @Test
    void categoryChangeAndEscalation() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        putJson(agent, "/api/v1/support/tickets/{id}/category", "{\"categoryCode\":\"DELIVERY\"}", id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.category.code").value("DELIVERY"));
        postJson(agent, "/api/v1/support/tickets/{id}/escalate", "{\"reason\":\"Montant important\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalated").value(true))
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("ESCALATE"))));
        getAs(customer, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.escalated").value(false));
    }

    @Test
    void duplicateMessageRetriesAreIgnored() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        String json = "{\"body\":\"Bonjour\",\"clientMessageId\":\"abc-123\"}";
        String first = json(postJson(customer, "/api/v1/support/tickets/{id}/messages", json, id)
                .andExpect(status().isCreated()), "$.id");
        String second = json(postJson(customer, "/api/v1/support/tickets/{id}/messages", json, id)
                .andExpect(status().isCreated()), "$.id");
        assertThat(second).isEqualTo(first);
        List<String> bodies = json(getAs(customer, "/api/v1/support/tickets/{id}/messages", id), "$[*].body");
        assertThat(bodies).filteredOn("Bonjour"::equals).hasSize(1);
    }
}
