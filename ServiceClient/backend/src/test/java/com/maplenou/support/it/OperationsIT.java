package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.application.port.AiAssistant;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** AI help inside tickets (DEC-CS11-08), rate limiting, metrics access and request ids (DEC-CS11-06). */
@TestPropertySource(properties = {
        "maplenou.rate-limit.enabled=true",
        "maplenou.rate-limit.writes-per-minute=15",
        "maplenou.metrics-token=test-metrics-token-0123456789"
})
class OperationsIT extends IntegrationTest {

    @MockitoBean
    AiAssistant assistant;

    @Test
    void theAssistantSuggestsACategoryAndPriorityThatTheAgentApplies() throws Exception {
        when(assistant.available()).thenReturn(true);
        when(assistant.triage(anyString(), anyString(), anyString(), anyList(), anyList()))
                .thenReturn(Optional.of(new AiAssistant.Triage("DELIVERY", "HIGH", 0.87, "Colis en retard", "test")));
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer, ticketJson("ORDER", "LOW").replace("Problème de test", "Colis en retard"));

        String category = null;
        for (int i = 0; i < 50 && category == null; i++) {
            java.util.Map<String, Object> suggestion = json(getAs(agent, "/api/v1/support/tickets/{id}", id), "$.aiSuggestion");
            category = suggestion == null ? null : (String) suggestion.get("categoryCode");
            if (category == null) {
                Thread.sleep(100);
            }
        }
        assertThat(category).isEqualTo("DELIVERY");
        // the requester never sees the suggestion
        getAs(customer, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.aiSuggestion").doesNotExist());
        postJson(customer, "/api/v1/support/tickets/{id}/ai/suggestion", "{\"apply\":true}", id)
                .andExpect(status().isForbidden());

        assignSelf(agent, id);
        postJson(agent, "/api/v1/support/tickets/{id}/ai/suggestion", "{\"apply\":true}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.code").value("DELIVERY"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.aiSuggestion.decision").value("APPLIED"));
        assertThat(body(getAs(agent, "/api/v1/support/tickets/{id}/history", id))).contains("AI_SUGGESTION_APPLIED");
    }

    @Test
    void agentsGetAReplyDraftTheyEditBeforeSending() throws Exception {
        when(assistant.available()).thenReturn(true);
        when(assistant.draftReply(anyString(), anyString(), anyList(), anyString(), any()))
                .thenReturn(Optional.of(new AiAssistant.Draft("Bonjour, votre colis est en route.",
                        List.of("Suivre une livraison"), "test")));
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        getAs(agent, "/api/v1/support/ai/status").andExpect(jsonPath("$.available").value(true));
        getAs(customer, "/api/v1/support/ai/status").andExpect(status().isForbidden());
        assignSelf(agent, id);
        postJson(agent, "/api/v1/support/tickets/{id}/ai/draft-reply", "{\"language\":\"fr\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Bonjour, votre colis est en route."))
                .andExpect(jsonPath("$.references[0]").value("Suivre une livraison"));
        postJson(customer, "/api/v1/support/tickets/{id}/ai/draft-reply", "{}", id).andExpect(status().isForbidden());

        when(assistant.available()).thenReturn(false);
        postJson(agent, "/api/v1/support/tickets/{id}/ai/draft-reply", "{}", id)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
    }

    @Test
    void tooManyWritesPerMinuteAreRefusedWith429() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        int refused = 0;
        String retryAfter = null;
        for (int i = 0; i < 20; i++) {
            var r = postJson(customer, "/api/v1/support/notifications/read-all", "").andReturn().getResponse();
            if (r.getStatus() == 429) {
                refused++;
                retryAfter = r.getHeader("Retry-After");
                assertThat(r.getContentAsString()).contains("TOO_MANY_REQUESTS");
            }
        }
        assertThat(refused).isGreaterThanOrEqualTo(5);
        assertThat(retryAfter).isNotBlank();
        // reads still work for another user
        getAs(newUser(Role.CUSTOMER), "/api/v1/support/notifications").andExpect(status().isOk());
    }

    @Test
    void prometheusMetricsNeedTheMetricsToken() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer wrong")).andExpect(status().isUnauthorized());
        String text = mvc.perform(get("/actuator/prometheus")
                        .header("Authorization", "Bearer test-metrics-token-0123456789"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(text).contains("support_outbox_pending").contains("application=\"maplenou-support\"");
    }

    @Test
    void everyResponseCarriesARequestId() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(header().exists("X-Request-Id"));
        mvc.perform(get("/actuator/health").header("X-Request-Id", "abc-12345"))
                .andExpect(header().string("X-Request-Id", "abc-12345"));
        // a forged, oversized id is replaced
        String forged = "x".repeat(300);
        String returned = mvc.perform(get("/actuator/health").header("X-Request-Id", forged))
                .andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(returned).isNotEqualTo(forged);
    }
}
