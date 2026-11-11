package com.maplenou.support.it;

import com.jayway.jsonpath.JsonPath;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.identity.application.port.AccessTokenIssuer;
import com.maplenou.support.identity.application.UserDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests: HTTP → security (real JWTs) → services → PostgreSQL with the Flyway schema.
 * Uses {@code TEST_DATABASE_URL} when set (local PostgreSQL), otherwise a Testcontainers PostgreSQL 16.
 * Each test creates its own users, so tests never depend on each other's data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "maplenou.security.jwt-secret=integration-test-secret-0123456789abcdef",
        "maplenou.security.dev-login-enabled=false",
        "maplenou.notifications.dispatcher-enabled=false",
        "maplenou.attachments.storage-dir=target/test-attachments",
        "maplenou.ecosystem.mode=demo"
})
public abstract class IntegrationTest {

    private static PostgreSQLContainer postgres;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        String url = System.getenv("TEST_DATABASE_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> env("TEST_DATABASE_USERNAME", "maplenou"));
            registry.add("spring.datasource.password", () -> env("TEST_DATABASE_PASSWORD", "maplenou"));
            return;
        }
        synchronized (IntegrationTest.class) {
            if (postgres == null) {
                postgres = new PostgreSQLContainer("postgres:16-alpine");
                postgres.start();
            }
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected AccessTokenIssuer tokens;
    @Autowired
    protected UserDirectory directory;

    // ------------------------------------------------------------------ users

    protected Actor newUser(Role role) {
        Actor u = new Actor(UUID.randomUUID(), role, role.name().toLowerCase() + "-"
                + UUID.randomUUID().toString().substring(0, 6), null, null);
        directory.sync(u);
        return u;
    }

    protected Actor existing(Actor u) {
        directory.sync(u);
        return u;
    }

    protected <B extends AbstractMockHttpServletRequestBuilder<B>> B as(Actor u, B req) {
        return req.header("Authorization", "Bearer " + tokens.issue(u).accessToken());
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected ResultActions getAs(Actor u, String url, Object... vars) throws Exception {
        return mvc.perform(as(u, get(url, vars)));
    }

    protected ResultActions postJson(Actor u, String url, String json, Object... vars) throws Exception {
        return mvc.perform(as(u, post(url, vars)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(Actor u, String url, String json, Object... vars) throws Exception {
        return mvc.perform(as(u, put(url, vars)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected static String body(ResultActions r) throws Exception {
        MvcResult res = r.andReturn();
        return res.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static <T> T json(ResultActions r, String path) throws Exception {
        return JsonPath.read(body(r), path);
    }

    // ------------------------------------------------------------------ domain helpers

    protected static String ticketJson(String category, String priority) {
        return """
                {"subject":"Problème de test","description":"Description détaillée du problème.",
                 "categoryCode":"%s","priority":%s,"urgent":false}
                """.formatted(category, priority == null ? "null" : "\"" + priority + "\"");
    }

    protected String createTicket(Actor requester) throws Exception {
        return createTicket(requester, ticketJson("ORDER", "MEDIUM"));
    }

    protected String createTicket(Actor requester, String json) throws Exception {
        ResultActions r = postJson(requester, "/api/v1/support/tickets", json).andExpect(status().isCreated());
        return json(r, "$.id");
    }

    protected void assignSelf(Actor agent, String ticketId) throws Exception {
        putJson(agent, "/api/v1/support/tickets/{id}/assign", "{}", ticketId).andExpect(status().isOk());
    }

    protected void setStatus(Actor user, String ticketId, String status) throws Exception {
        putJson(user, "/api/v1/support/tickets/{id}/status", "{\"status\":\"" + status + "\"}", ticketId)
                .andExpect(status().isOk());
    }

    protected void reply(Actor user, String ticketId, String text) throws Exception {
        postJson(user, "/api/v1/support/tickets/{id}/messages", "{\"body\":\"" + text + "\"}", ticketId)
                .andExpect(status().isCreated());
    }

    /** Creates a ticket and brings it to RESOLVED through the normal workflow. */
    protected String resolvedTicket(Actor requester, Actor agent) throws Exception {
        String id = createTicket(requester);
        assignSelf(agent, id);
        reply(agent, id, "Nous traitons votre demande.");
        setStatus(agent, id, "RESOLVED");
        return id;
    }
}
