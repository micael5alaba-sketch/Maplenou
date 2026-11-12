package com.maplenou.support.it;

import com.maplenou.support.demo.DemoUsers;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.ticket.application.AutoCloseService;
import com.maplenou.support.ticket.application.VocabularyService;
import com.maplenou.support.ticket.domain.vocabulary.StatusDefinition;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import com.maplenou.support.ticket.domain.vocabulary.Transition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Iteration 6 features (UPGRADE-PLAN): SLA pause, auto-close, satisfaction, canned replies, CSV export,
 * configurable vocabulary, actions on Maplenou (demo adapter) and the transactional outbox.
 */
class SupportFeaturesIT extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AutoCloseService autoClose;
    @Autowired
    VocabularyService vocabulary;
    @Autowired
    JsonMapper mapper;

    private String history(Actor u, String id) throws Exception {
        return body(getAs(u, "/api/v1/support/tickets/{id}/history", id));
    }

    // ---------------------------------------------------------------- DEC-CS11-03 SLA pause

    @Test
    void waitingForTheCustomerPausesTheResolutionClockAndExtendsTheDueDate() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Pouvez-vous envoyer une photo ?");
        setStatus(agent, id, "WAITING_FOR_CUSTOMER");

        var paused = getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.sla.pausedSince").isNotEmpty())
                .andExpect(jsonPath("$.sla.resolution.state").value("PAUSED"))
                .andExpect(jsonPath("$.statusKind").value("WAITING_REQUESTER"));
        Instant dueBefore = Instant.parse(json(paused, "$.sla.resolution.dueAt"));

        // the customer took two hours to answer
        jdbc.update("UPDATE support_tickets SET sla_paused_since = sla_paused_since - INTERVAL '2 hours' WHERE id = ?",
                UUID.fromString(id));
        reply(customer, id, "Voici la photo");

        var resumed = getAs(agent, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.sla.pausedSince").doesNotExist());
        Instant dueAfter = Instant.parse(json(resumed, "$.sla.resolution.dueAt"));
        assertThat(Duration.between(dueBefore, dueAfter).toMinutes()).isBetween(119L, 121L);
        assertThat(history(agent, id)).contains("SLA_PAUSED", "SLA_RESUMED");
    }

    // ---------------------------------------------------------------- DEC-CS11-04 auto-close

    @Test
    void resolvedTicketsCloseAutomaticallyAfterSevenDays() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String old = resolvedTicket(customer, agent);
        String recent = resolvedTicket(customer, agent);
        jdbc.update("UPDATE support_tickets SET resolved_at = now() - INTERVAL '8 days' WHERE id = ?",
                UUID.fromString(old));

        assertThat(autoClose.enabled()).isTrue();
        assertThat(autoClose.closeDue()).isGreaterThanOrEqualTo(1);

        getAs(customer, "/api/v1/support/tickets/{id}", old)
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());
        assertThat(history(agent, old)).contains("AUTO_CLOSED");
        getAs(customer, "/api/v1/support/tickets/{id}", recent).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    // ---------------------------------------------------------------- DEC-CS11-05 satisfaction

    @Test
    void theRequesterRatesAResolvedTicketOnce() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String open = createTicket(customer);
        postJson(customer, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":5}", open)
                .andExpect(status().isForbidden());

        String id = resolvedTicket(customer, agent);
        getAs(customer, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.hasItem("RATE")));
        postJson(agent, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":5}", id)
                .andExpect(status().isForbidden());
        postJson(customer, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":9}", id)
                .andExpect(status().isBadRequest());
        postJson(customer, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":4,\"comment\":\"Rapide\"}", id)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Rapide"));
        getAs(agent, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.satisfaction.rating").value(4));
        postJson(customer, "/api/v1/support/tickets/{id}/satisfaction", "{\"rating\":1}", id)
                .andExpect(status().isForbidden());
        assertThat(history(agent, id)).contains("RATED");
        getAs(newUser(Role.SUPPORT_MANAGER), "/api/v1/support/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.satisfactionLast30Days.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ---------------------------------------------------------------- canned replies

    @Test
    void managersMaintainCannedRepliesThatAgentsUse() throws Exception {
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String title = "Délai " + UUID.randomUUID().toString().substring(0, 8);
        String reply = "{\"title\":\"" + title + "\",\"body\":\"Votre colis arrive sous 48 h.\",\"language\":\"fr\","
                + "\"categoryCode\":\"DELIVERY\",\"active\":true}";
        postJson(agent, "/api/v1/support/canned-replies", reply).andExpect(status().isForbidden());
        String id = json(postJson(manager, "/api/v1/support/canned-replies", reply).andExpect(status().isCreated()),
                "$.id");
        List<String> titles = json(getAs(agent, "/api/v1/support/canned-replies"), "$[*].title");
        assertThat(titles).contains(title);
        putJson(manager, "/api/v1/support/canned-replies/{id}", reply.replace("\"active\":true", "\"active\":false"), id)
                .andExpect(status().isOk());
        titles = json(getAs(agent, "/api/v1/support/canned-replies"), "$[*].title");
        assertThat(titles).doesNotContain(title);
        getAs(newUser(Role.CUSTOMER), "/api/v1/support/canned-replies").andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- CSV export

    @Test
    void managersExportTicketsAsCsvWithoutFormulaInjection() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String json = ticketJson("ORDER", "LOW").replace("Problème de test", "=HYPERLINK(\\\"http://x\\\")");
        createTicket(customer, json);
        getAs(newUser(Role.SUPPORT_AGENT), "/api/v1/support/tickets/export.csv").andExpect(status().isForbidden());
        var csv = getAs(newUser(Role.SUPPORT_MANAGER), "/api/v1/support/tickets/export.csv?requesterId=" + customer.id())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse();
        assertThat(csv.getHeader("Content-Disposition")).contains("attachment").contains(".csv");
        String text = csv.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿");
        assertThat(text).contains("TKT-");
        assertThat(text).contains("'=HYPERLINK").doesNotContain(";=HYPERLINK").doesNotContain(";\"=HYPERLINK");
        getAs(newUser(Role.SUPPORT_MANAGER), "/api/v1/support/tickets/export.csv?status=NOPE")
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- DEC-CS11-02 configurable vocabulary

    @Test
    void administratorsAddAStatusAndTheWorkflowUsesIt() throws Exception {
        Actor admin = newUser(Role.ADMINISTRATOR);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor customer = newUser(Role.CUSTOMER);
        List<StatusDefinition> original = vocabulary.workflow().statuses();
        List<Transition> originalTransitions = vocabulary.workflow().transitions();

        List<Map<String, Object>> statuses = new ArrayList<>();
        for (StatusDefinition s : original) {
            statuses.add(statusInput(s.code(), s.kind(), s.labelFr(), s.labelEn(), s.sortOrder(), s.slaPaused()));
        }
        statuses.add(statusInput("WAITING_SELLER", StatusKind.ON_HOLD, "En attente du vendeur", "Waiting for the seller",
                45, true));
        List<Map<String, String>> transitions = new ArrayList<>();
        originalTransitions.forEach(t -> transitions.add(Map.of("from", t.from(), "to", t.to())));
        transitions.add(Map.of("from", "IN_PROGRESS", "to", "WAITING_SELLER"));
        transitions.add(Map.of("from", "WAITING_SELLER", "to", "IN_PROGRESS"));
        String body = mapper.writeValueAsString(Map.of("statuses", statuses, "transitions", transitions));

        putJson(agent, "/api/v1/support/admin/workflow", body).andExpect(status().isForbidden());
        String id = createTicket(customer);
        try {
            putJson(admin, "/api/v1/support/admin/workflow", body)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statuses[?(@.code=='WAITING_SELLER')].kind").value("ON_HOLD"));
            getAs(customer, "/api/v1/support/vocabulary")
                    .andExpect(jsonPath("$.statuses[?(@.code=='WAITING_SELLER')].labelEn").value("Waiting for the seller"));

            assignSelf(agent, id);
            reply(agent, id, "Nous contactons le vendeur.");
            setStatus(agent, id, "WAITING_SELLER");
            getAs(agent, "/api/v1/support/tickets/{id}", id)
                    .andExpect(jsonPath("$.status").value("WAITING_SELLER"))
                    .andExpect(jsonPath("$.statusKind").value("ON_HOLD"))
                    .andExpect(jsonPath("$.sla.resolution.state").value("PAUSED"));
            // a status still used by a ticket cannot be removed
            String withoutCustom = mapper.writeValueAsString(Map.of("statuses", statuses.subList(0, original.size()),
                    "transitions", transitions.subList(0, originalTransitions.size())));
            putJson(admin, "/api/v1/support/admin/workflow", withoutCustom).andExpect(status().is4xxClientError());
            setStatus(agent, id, "IN_PROGRESS");

            // the lifecycle rules are checked: a workflow without a CLOSED status is refused
            List<Map<String, Object>> broken = new ArrayList<>(statuses);
            broken.removeIf(s -> s.get("kind") == StatusKind.CLOSED);
            putJson(admin, "/api/v1/support/admin/workflow",
                    mapper.writeValueAsString(Map.of("statuses", broken, "transitions", transitions)))
                    .andExpect(status().is4xxClientError());
        } finally {
            String restore = mapper.writeValueAsString(Map.of("statuses", statuses.subList(0, original.size()),
                    "transitions", transitions.subList(0, originalTransitions.size())));
            putJson(admin, "/api/v1/support/admin/workflow", restore).andExpect(status().isOk());
        }
        getAs(customer, "/api/v1/support/vocabulary")
                .andExpect(jsonPath("$.statuses[?(@.code=='WAITING_SELLER')]").isEmpty());
    }

    @Test
    void anUnsafePermissionMatrixIsRefused() throws Exception {
        Actor admin = newUser(Role.ADMINISTRATOR);
        // requesters may never see every ticket
        putJson(admin, "/api/v1/support/admin/permissions",
                "[{\"role\":\"CUSTOMER\",\"action\":\"VIEW\",\"scope\":\"ANY\"}]")
                .andExpect(status().is4xxClientError());
        getAs(admin, "/api/v1/support/admin/permissions")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rules").isNotEmpty())
                .andExpect(jsonPath("$.defaults").isNotEmpty());
    }

    private static Map<String, Object> statusInput(String code, StatusKind kind, String fr, String en, int order,
                                              boolean paused) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("code", code);
        m.put("kind", kind);
        m.put("labelFr", fr);
        m.put("labelEn", en);
        m.put("sortOrder", order);
        m.put("slaPaused", paused);
        m.put("active", true);
        return m;
    }

    // ---------------------------------------------------------------- DEC-CS11-07 actions on Maplenou (demo)

    @Test
    void agentsAskMaplenouForARefundAndTheActionIsAudited() throws Exception {
        Actor jean = existing(DemoUsers.byId(DemoUsers.CUSTOMER_JEAN));
        Actor agent = newUser(Role.SUPPORT_AGENT);
        String id = createTicket(jean, """
                {"subject":"Colis abîmé","description":"Le produit est cassé.","categoryCode":"RETURN_REFUND",
                 "orderRef":"CMD-2026-0456"}
                """);
        String refund = "{\"reason\":\"Produit reçu cassé, photo jointe au ticket.\",\"productCondition\":\"DAMAGED\"}";
        postJson(jean, "/api/v1/support/tickets/{id}/actions/refund-request", refund, id)
                .andExpect(status().isForbidden());
        assignSelf(agent, id);
        postJson(agent, "/api/v1/support/tickets/{id}/actions/refund-request", refund, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalActions[0].type").value("REFUND_REQUEST"))
                .andExpect(jsonPath("$.externalActions[0].succeeded").value(true))
                .andExpect(jsonPath("$.externalActions[0].resultRef").value(org.hamcrest.Matchers.startsWith("RMB-DEMO-")));
        postJson(agent, "/api/v1/support/tickets/{id}/actions/contact-driver",
                "{\"message\":\"Merci de rappeler le client.\"}", id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalActions.length()").value(2));
        assertThat(history(agent, id)).contains("EXTERNAL_ACTION");
        // the requester never sees the audit of operational actions
        getAs(jean, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.externalActions").isEmpty());

        String noOrder = createTicket(newUser(Role.CUSTOMER));
        assignSelf(agent, noOrder);
        postJson(agent, "/api/v1/support/tickets/{id}/actions/refund-request", refund, noOrder)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NO_ORDER_LINKED"));
    }

    // ---------------------------------------------------------------- DEC-CS11-09 templates per language

    @Test
    void notificationsUseTheRecipientsLanguageAndAdministratorsEditTheTexts() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        Actor agent = newUser(Role.SUPPORT_AGENT);
        Actor admin = newUser(Role.ADMINISTRATOR);
        putJson(customer, "/api/v1/support/me/preferences", "{\"language\":\"en\"}").andExpect(status().isNoContent());
        putJson(customer, "/api/v1/support/me/preferences", "{\"language\":\"de\"}").andExpect(status().isBadRequest());
        String id = createTicket(customer);
        assignSelf(agent, id);
        reply(agent, id, "Hello");
        Thread.sleep(300); // notifications come from the outbox, right after commit
        List<String> titles = json(getAs(customer, "/api/v1/support/notifications?size=50"), "$.items[*].title");
        assertThat(titles).contains("New reply");

        String template = "{\"title\":\"Une réponse vous attend\",\"body\":\"Ticket {ticket} : nouvelle réponse.\"}";
        putJson(agent, "/api/v1/support/admin/notification-templates/AGENT_REPLIED/fr", template)
                .andExpect(status().isForbidden());
        try {
            putJson(admin, "/api/v1/support/admin/notification-templates/AGENT_REPLIED/fr", template)
                    .andExpect(status().isOk());
            Actor french = newUser(Role.CUSTOMER);
            String other = createTicket(french);
            assignSelf(agent, other);
            reply(agent, other, "Bonjour");
            Thread.sleep(300);
            List<String> bodies = json(getAs(french, "/api/v1/support/notifications?size=50"), "$.items[*].body");
            assertThat(bodies).anyMatch(b -> b.startsWith("Ticket TKT-") && b.endsWith(": nouvelle réponse."));
        } finally {
            putJson(admin, "/api/v1/support/admin/notification-templates/AGENT_REPLIED/fr",
                    "{\"title\":\"Nouvelle réponse\",\"body\":\"Votre ticket {ticket} a reçu une nouvelle réponse.\"}")
                    .andExpect(status().isOk());
        }
    }

    // ---------------------------------------------------------------- DEC-CS11-01 transactional outbox

    @Test
    void domainEventsAreStoredInTheOutboxAndDeliveredAfterCommit() throws Exception {
        Actor customer = newUser(Role.CUSTOMER);
        String id = createTicket(customer);
        String like = "%" + id + "%";
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE payload LIKE ?", Long.class, like))
                .isGreaterThanOrEqualTo(1);
        long pending = -1;
        for (int i = 0; i < 50; i++) {
            pending = jdbc.queryForObject("SELECT count(*) FROM outbox_events WHERE payload LIKE ? AND published_at IS NULL",
                    Long.class, like);
            if (pending == 0) {
                break;
            }
            Thread.sleep(100);
        }
        assertThat(pending).isZero();
        List<String> types = json(getAs(customer, "/api/v1/support/notifications?size=50"), "$.items[*].type");
        assertThat(types).contains("TICKET_CREATED");
    }
}
