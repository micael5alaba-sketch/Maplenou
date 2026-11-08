package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FR-012 — AC-003, AC-007, AC-023, AC-024, BR-070..BR-076. */
class AccessControlIT extends IntegrationTest {

    @Test
    void ac003_requesterCannotSeeAnotherRequestersTicket() throws Exception {
        Actor owner = newUser(Role.CUSTOMER);
        Actor other = newUser(Role.CUSTOMER);
        String id = createTicket(owner);
        getAs(other, "/api/v1/support/tickets/{id}", id).andExpect(status().isNotFound());
        getAs(other, "/api/v1/support/tickets/{id}/messages", id).andExpect(status().isNotFound());
        getAs(other, "/api/v1/support/tickets/{id}/history", id).andExpect(status().isNotFound());
        postJson(other, "/api/v1/support/tickets/{id}/messages", "{\"body\":\"hi\"}", id)
                .andExpect(status().isNotFound());
        List<String> ids = json(getAs(other, "/api/v1/support/tickets"), "$.items[*].id");
        assertThat(ids).doesNotContain(id);
    }

    @Test
    void ac003_requesterFiltersCannotWidenScope() throws Exception {
        Actor owner = newUser(Role.CUSTOMER);
        Actor other = newUser(Role.CUSTOMER);
        createTicket(owner);
        getAs(other, "/api/v1/support/tickets?requesterId={id}", owner.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void ac007_internalNotesNeverReachRequesters() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        // internal note with an attachment
        byte[] pdf = "%PDF-1.7 note".getBytes();
        String noteJson = json(mvc.perform(as(agent, multipart("/api/v1/support/tickets/{id}/internal-notes", id)
                        .file(new MockMultipartFile("message", "", MediaType.APPLICATION_JSON_VALUE,
                                "{\"body\":\"Client déjà informé\"}".getBytes()))
                        .file(new MockMultipartFile("files", "interne.pdf", "application/pdf", pdf))))
                .andExpect(status().isCreated()), "$.attachments[0].id");

        List<String> visibilities = json(getAs(customer, "/api/v1/support/tickets/{id}/messages", id), "$[*].visibility");
        assertThat(visibilities).containsOnly("PUBLIC");
        // even explicitly asking for internal notes returns public messages only
        List<String> asked = json(getAs(customer, "/api/v1/support/tickets/{id}/messages?visibility=INTERNAL", id),
                "$[*].visibility");
        assertThat(asked).doesNotContain("INTERNAL");
        getAs(customer, "/api/v1/support/tickets/{id}/attachments/{a}", id, noteJson).andExpect(status().isNotFound());
        postJson(customer, "/api/v1/support/tickets/{id}/internal-notes", "{\"body\":\"x\"}", id)
                .andExpect(status().isForbidden());

        List<String> supportView = json(getAs(agent, "/api/v1/support/tickets/{id}/messages", id), "$[*].visibility");
        assertThat(supportView).contains("INTERNAL");
        getAs(agent, "/api/v1/support/tickets/{id}/attachments/{a}", id, noteJson).andExpect(status().isOk());
    }

    @Test
    void ac023_agentCanViewButNotModifyAnotherAgentsTicket() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor owner = newUser(Role.SUPPORT_AGENT);
        Actor other = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(owner, id);

        getAs(other, "/api/v1/support/tickets/{id}", id).andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.containsInAnyOrder(
                        "VIEW_INTERNAL", "VIEW_CONTEXT", "VIEW_SLA")));
        postJson(other, "/api/v1/support/tickets/{id}/messages", "{\"body\":\"x\"}", id).andExpect(status().isForbidden());
        putJson(other, "/api/v1/support/tickets/{id}/status", "{\"status\":\"IN_PROGRESS\"}", id)
                .andExpect(status().isForbidden());
        putJson(other, "/api/v1/support/tickets/{id}/assign", "{}", id)
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.rule").value("BR-058"));
        putJson(other, "/api/v1/support/tickets/{id}/priority", "{\"priority\":\"LOW\"}", id)
                .andExpect(status().isForbidden());
    }

    @Test
    void ac024_agentCannotAdministerSystem() throws Exception {
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor other = newUser(Role.SUPPORT_AGENT);
        putJson(agent, "/api/v1/support/admin/agents/{id}/active", "{\"active\":false}", other.id())
                .andExpect(status().isForbidden());
        putJson(agent, "/api/v1/support/admin/categories/ORDER",
                "{\"name\":\"X\",\"description\":null,\"active\":true}").andExpect(status().isForbidden());
        putJson(agent, "/api/v1/support/admin/holidays/2026-12-25", "{\"label\":\"Noël\"}").andExpect(status().isForbidden());
        getAs(agent, "/api/v1/support/reports/summary?from=2026-01-01&to=2026-01-31").andExpect(status().isForbidden());
    }

    @Test
    void requestersCannotUseSupportScreens() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        getAs(customer, "/api/v1/support/dashboard").andExpect(status().isForbidden());
        getAs(customer, "/api/v1/support/agents").andExpect(status().isForbidden());
        getAs(customer, "/api/v1/support/admin/agents").andExpect(status().isForbidden());
    }

    @Test
    void deactivatedSupportAccountIsBlocked() throws Exception {
        Actor admin = newUser(Role.ADMINISTRATOR);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        putJson(admin, "/api/v1/support/admin/agents/{id}/active", "{\"active\":false}", agent.id())
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        getAs(agent, "/api/v1/support/tickets").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        putJson(admin, "/api/v1/support/admin/agents/{id}/active", "{\"active\":true}", agent.id())
                .andExpect(status().isOk());
        getAs(agent, "/api/v1/support/tickets").andExpect(status().isOk());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/support/tickets").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requesterSeesOnlyRequesterVisibleHistory() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        postJson(agent, "/api/v1/support/tickets/{id}/internal-notes", "{\"body\":\"note\"}", id)
                .andExpect(status().isCreated());
        List<String> requesterActions = json(getAs(customer, "/api/v1/support/tickets/{id}/history", id), "$[*].action");
        assertThat(requesterActions).contains("CREATED", "STATUS_CHANGED")
                .doesNotContain("INTERNAL_NOTE_ADDED", "ASSIGNED");
        List<String> supportActions = json(getAs(agent, "/api/v1/support/tickets/{id}/history", id), "$[*].action");
        assertThat(supportActions).contains("INTERNAL_NOTE_ADDED", "ASSIGNED");
    }
}
