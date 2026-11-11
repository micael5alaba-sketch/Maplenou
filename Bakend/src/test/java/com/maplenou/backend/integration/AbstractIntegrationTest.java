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


    // ----- Comptes du personnel (rôles du back-office) -----

    /** Session d'un compte du personnel apres onboarding complet (mot de passe change + MFA active). */
    protected record StaffSession(String token, String phone, String password, String totpSecret, String userId, String email) {
        /** Code TOTP courant (pour la reconnexion MFA ou l'approbation d'un retrait). */
        public String code() throws Exception {
            return new dev.samstevens.totp.code.DefaultCodeGenerator().generate(totpSecret,
                    new dev.samstevens.totp.time.SystemTimeProvider().getTime() / 30);
        }
    }

    /**
     * Crée un compte du personnel via /api/admin/staff puis déroule la première connexion
     * complète : changement du mot de passe initial, activation de la MFA, reconnexion avec MFA.
     */
    protected StaffSession createStaffWithMfa(String superAdminToken, String role, boolean deliveryPricingGranted) throws Exception {
        long rand = System.nanoTime();
        String phone = "+22860" + String.format("%07d", rand % 10_000_000L);

        String email = "staff" + rand + "@maplenou.test";
        JsonNode created = post("/api/admin/staff", java.util.Map.of(
                "fullName", "Staff " + role,
                "phoneNumber", phone,
                "email", email,
                "role", role,
                "deliveryPricingGranted", deliveryPricingGranted), superAdminToken, 201);
        String temporaryPassword = created.get("temporaryPassword").asText();
        String userId = created.get("staff").get("id").asText();

        String firstToken = login(phone, temporaryPassword);
        String newPassword = "StaffNew1pass";
        patch("/api/users/me/password", java.util.Map.of(
                "currentPassword", temporaryPassword, "newPassword", newPassword), firstToken, 204);

        JsonNode setup = post("/api/auth/2fa/setup", null, firstToken, 200);
        String secret = setup.get("secret").asText();
        String code = new dev.samstevens.totp.code.DefaultCodeGenerator().generate(secret,
                new dev.samstevens.totp.time.SystemTimeProvider().getTime() / 30);
        JsonNode activated = post("/api/auth/2fa/activate", java.util.Map.of("code", code), firstToken, 200);
        org.junit.jupiter.api.Assertions.assertEquals(10, activated.get("recoveryCodes").size());

        JsonNode challenge = post("/api/auth/login",
                new LoginRequest(phone, newPassword), null, 200);
        org.junit.jupiter.api.Assertions.assertTrue(challenge.get("mfaRequired").asBoolean(),
                "la MFA doit etre exigee a la connexion");
        String mfaToken = challenge.get("mfaToken").asText();
        JsonNode full = post("/api/auth/2fa/verify-login", java.util.Map.of("mfaToken", mfaToken, "code", code), null, 200);
        return new StaffSession(full.get("accessToken").asText(), phone, newPassword, secret, userId, email);
    }

    // Mécanisme simple pour ajouter un en-tête personnalisé (ex: X-Webhook-Secret) au prochain appel.
    private String extraHeaderName;
    private String extraHeaderValue;
}
