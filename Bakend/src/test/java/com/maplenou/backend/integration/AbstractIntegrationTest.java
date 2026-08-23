package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maplenou.backend.auth.dto.LoginRequest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Base commune pour les tests d'intégration bout-en-bout.
 *
 * Utilise java.net.http.HttpClient (JDK) plutôt que TestRestTemplate : cette classe n'existe
 * plus dans spring-boot-test à partir de Spring Boot 4 (remplacée par RestTestClient, dont
 * l'API façon WebTestClient diffère significativement et n'est pas auto-configurée).
 *
 * IMPORTANT : comme le reste des tests de ce projet (pas de Testcontainers), ces tests tournent
 * contre PostgreSQL ET Redis réels configurés dans application.properties — les deux doivent
 * être démarrés localement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// Ces tests enregistrent/connectent de nombreux comptes en quelques secondes depuis la même IP,
// ce qui dépasserait la limite de production (10/min) sans changer le comportement testé.
@TestPropertySource(properties = "app.rate-limit.auth.max-per-minute=100000")
public abstract class AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    protected final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    protected String login(String phone, String password) throws Exception {
        JsonNode body = post("/api/auth/login", new LoginRequest(phone, password), null, 200);
        return body.get("accessToken").asText();
    }

    protected JsonNode post(String path, Object body, String token, int expectedStatus) throws Exception {
        return exchange("POST", path, body, token, expectedStatus);
    }

    protected JsonNode put(String path, Object body, String token, int expectedStatus) throws Exception {
        return exchange("PUT", path, body, token, expectedStatus);
    }

    protected JsonNode patch(String path, Object body, String token, int expectedStatus) throws Exception {
        return exchange("PATCH", path, body, token, expectedStatus);
    }

    protected JsonNode get(String path, String token, int expectedStatus) throws Exception {
        return exchange("GET", path, null, token, expectedStatus);
    }

    protected JsonNode delete(String path, String token, int expectedStatus) throws Exception {
        return exchange("DELETE", path, null, token, expectedStatus);
    }

    protected JsonNode postWithHeader(String path, Object body, String headerName, String headerValue, int expectedStatus) throws Exception {
        this.extraHeaderName = headerName;
        this.extraHeaderValue = headerValue;
        return exchange("POST", path, body, null, expectedStatus);
    }

    protected JsonNode exchange(String method, String path, Object body, String token, int expectedStatus) throws Exception {
        String json = body != null ? objectMapper.writeValueAsString(body) : "";
        BodyPublisher publisher = body != null ? BodyPublishers.ofString(json) : BodyPublishers.noBody();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, publisher);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (extraHeaderName != null) builder.header(extraHeaderName, extraHeaderValue);
        extraHeaderName = null;

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(expectedStatus, response.statusCode(),
                () -> method + " " + path + " -> " + response.statusCode() + " body=" + response.body());

        String responseBody = response.body();
        if (responseBody == null || responseBody.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(responseBody);
    }

    // Mécanisme simple pour ajouter un en-tête personnalisé (ex: X-Webhook-Secret) au prochain appel.
    private String extraHeaderName;
    private String extraHeaderValue;
}
