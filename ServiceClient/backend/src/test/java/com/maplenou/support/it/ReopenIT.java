package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-017, DEC-004, DEC-007 — AC-019, AC-020, AC-021, BR-048..BR-055. */
class ReopenIT extends IntegrationTest {

    private String requestReopen(Actor requester, String id) throws Exception {
        return json(postJson(requester, "/api/v1/support/tickets/{id}/reopen-requests",
                "{\"reason\":\"Le problème persiste\"}", id).andExpect(status().isCreated()), "$.id");
    }

    @Test
    void ac019_requesterRequestsReopeningOfResolvedTicket() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = resolvedTicket(customer, agent);
        requestReopen(customer, id);
        getAs(customer, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("RESOLVED")) // BR-049: no direct transition
                .andExpect(jsonPath("$.pendingReopenRequest.status").value("PENDING"))
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("REQUEST_REOPEN"))));
        getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.hasItem("DECIDE_REOPEN")));
        // second request while one is pending
        postJson(customer, "/api/v1/support/tickets/{id}/reopen-requests", "{\"reason\":\"encore\"}", id)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REOPEN_ALREADY_PENDING"));
        // the ticket cannot be closed while the request is pending
        putJson(agent, "/api/v1/support/tickets/{id}/status", "{\"status\":\"CLOSED\"}", id)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REOPEN_PENDING"));
    }

    @Test
    void ac020_approvedReopeningReturnsToInProgressWithNewSlaPeriod() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = resolvedTicket(customer, agent);
        String requestId = requestReopen(customer, id);
        putJson(agent, "/api/v1/support/tickets/{id}/reopen-requests/{r}", "{\"decision\":\"APPROVE\",\"note\":\"OK\"}",
                id, requestId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.pendingReopenRequest").doesNotExist())
                .andExpect(jsonPath("$.lastReopenDecision.status").value("APPROVED"))
                .andExpect(jsonPath("$.sla.periods.length()").value(2))
                .andExpect(jsonPath("$.sla.periods[0].endReason").value("RESOLVED"))
                .andExpect(jsonPath("$.sla.periods[1].endedAt").doesNotExist());
        List<String> actions = json(getAs(customer, "/api/v1/support/tickets/{id}/history", id), "$[*].action");
        assertThat(actions).contains("REOPEN_REQUESTED", "REOPEN_APPROVED");
    }

    @Test
    void rejectedReopeningKeepsTicketResolved() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = resolvedTicket(customer, agent);
        String requestId = requestReopen(customer, id);
        putJson(agent, "/api/v1/support/tickets/{id}/reopen-requests/{r}",
                "{\"decision\":\"REJECT\",\"note\":\"Problème déjà traité\"}", id, requestId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.lastReopenDecision.status").value("REJECTED"));
        // the agent may now close it
        setStatus(agent, id, "CLOSED");
    }

    @Test
    void ac021_closedTicketCannotBeReopenedByRequester() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = resolvedTicket(customer, agent);
        setStatus(agent, id, "CLOSED");
        postJson(customer, "/api/v1/support/tickets/{id}/reopen-requests", "{\"reason\":\"svp\"}", id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TICKET_CLOSED"))
                .andExpect(jsonPath("$.rule").value("BR-052"));
    }

    @Test
    void onlyTheRequesterMayAskAndOnlyForResolvedTickets() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor other = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String open = createTicket(customer);
        postJson(customer, "/api/v1/support/tickets/{id}/reopen-requests", "{\"reason\":\"x\"}", open)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TICKET_NOT_RESOLVED"));
        String resolved = resolvedTicket(customer, agent);
        postJson(other, "/api/v1/support/tickets/{id}/reopen-requests", "{\"reason\":\"x\"}", resolved)
                .andExpect(status().isNotFound());
        postJson(agent, "/api/v1/support/tickets/{id}/reopen-requests", "{\"reason\":\"x\"}", resolved)
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherAgentCannotDecide() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor other = newUser(Role.SUPPORT_AGENT);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        String id = resolvedTicket(customer, agent);
        String requestId = requestReopen(customer, id);
        putJson(other, "/api/v1/support/tickets/{id}/reopen-requests/{r}", "{\"decision\":\"APPROVE\"}", id, requestId)
                .andExpect(status().isForbidden());
        putJson(manager, "/api/v1/support/tickets/{id}/reopen-requests/{r}", "{\"decision\":\"APPROVE\"}", id, requestId)
                .andExpect(status().isOk());
    }
}
