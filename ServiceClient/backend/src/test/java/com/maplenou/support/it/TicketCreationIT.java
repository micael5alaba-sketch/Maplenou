package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.sla.application.SlaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-001, FR-002, FR-016 — AC-001, AC-002, AC-013, AC-014, AC-015. */
class TicketCreationIT extends IntegrationTest {

    @Autowired
    SlaService sla;

    @Test
    void ac001_createsTicketAsNewWithNumberAndSla() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        postJson(customer, "/api/v1/support/tickets", ticketJson("DELIVERY", "HIGH"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.ticketNumber").value(org.hamcrest.Matchers.matchesPattern("TKT-\\d{4}-\\d{5,}")))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.category.code").value("DELIVERY"))
                .andExpect(jsonPath("$.requester.id").value(customer.id().toString()))
                .andExpect(jsonPath("$.sla").doesNotExist()) // SLA is support information
                .andExpect(jsonPath("$.allowedActions[0]").value("SEND_MESSAGE"));
    }

    @Test
    void ac001_ticketNumbersAreUnique() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String a = json(getAs(customer, "/api/v1/support/tickets/{id}", createTicket(customer)), "$.ticketNumber");
        String b = json(getAs(customer, "/api/v1/support/tickets/{id}", createTicket(customer)), "$.ticketNumber");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void sellersAndDriversCanCreateTickets() throws Exception {
        createTicket(newUser(Role.SELLER), ticketJson("PAYMENT", null));
        createTicket(newUser(Role.DELIVERY_DRIVER), ticketJson("DELIVERY", null));
    }

    @Test
    void ac002_unauthenticatedCreationIsRejected() throws Exception {
        mvc.perform(post("/api/v1/support/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("ORDER", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supportAgentsCannotCreateTickets() throws Exception {
        postJson(newUser(Role.SUPPORT_AGENT), "/api/v1/support/tickets", ticketJson("ORDER", null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.rule").value("BR-003"));
    }

    @Test
    void br004_requesterComesFromTokenNotBody() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor victim = newUser(Role.CUSTOMER);
        String json = """
                {"subject":"x","description":"y","categoryCode":"OTHER","requesterId":"%s"}
                """.formatted(victim.id());
        postJson(customer, "/api/v1/support/tickets", json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requester.id").value(customer.id().toString()));
    }

    @Test
    void ac013_slaTargetsComputedFromCreationAndPriority() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer, ticketJson("ORDER", "LOW"));
        var detail = getAs(agent, "/api/v1/support/tickets/{id}", id).andExpect(status().isOk());
        Instant created = Instant.parse(json(detail, "$.createdAt"));
        assertThat(Instant.parse((String) json(detail, "$.sla.firstResponse.dueAt")))
                .isEqualTo(sla.firstResponseDue(created, "LOW"));
        assertThat(Instant.parse((String) json(detail, "$.sla.resolution.dueAt")))
                .isEqualTo(sla.resolutionDue(created, "LOW"));
        assertThat((String) json(detail, "$.sla.firstResponse.state")).isIn("ON_TRACK", "APPROACHING");
    }

    @Test
    void ac014_reportedUrgencyDoesNotSetCritical() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String json = """
                {"subject":"Fraude","description":"Paiement suspect","categoryCode":"PAYMENT","urgent":true}
                """;
        postJson(customer, "/api/v1/support/tickets", json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.requesterReportedUrgent").value(true));
    }

    @Test
    void ac015_requesterCannotSetCritical() throws Exception {
        postJson(newUser(Role.CUSTOMER), "/api/v1/support/tickets", ticketJson("PAYMENT", "CRITICAL"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CRITICAL_PRIORITY_RESERVED"))
                .andExpect(jsonPath("$.rule").value("BR-036"));
    }

    @Test
    void validationErrorsAreReported() throws Exception {
        postJson(newUser(Role.CUSTOMER), "/api/v1/support/tickets",
                "{\"subject\":\"\",\"description\":\"d\",\"categoryCode\":\"ORDER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.subject").exists());
        postJson(newUser(Role.CUSTOMER), "/api/v1/support/tickets", ticketJson("UNKNOWN", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CATEGORY"));
    }

    @Test
    void categoriesAreListed() throws Exception {
        getAs(newUser(Role.CUSTOMER), "/api/v1/support/categories")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].code").value("ORDER"));
    }
}
