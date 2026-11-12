package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.support.dto.CreateSupportTicketRequest;
import com.maplenou.backend.supportbridge.SupportBridgeTokenFilter;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pont interne vers le Service client (ServiceClient/) : jeton de service obligatoire, lecture seule,
 * format attendu par HttpEcosystemGateway et par l'import des anciens tickets.
 */
@TestPropertySource(properties = "app.support-bridge.token=test-bridge-token-0123456789abcdef")
class SupportBridgeIntegrationTest extends AbstractIntegrationTest {

    private static final String TOKEN = "test-bridge-token-0123456789abcdef";
    private final HttpClient http = HttpClient.newHttpClient();
    private final long rand = System.nanoTime() % 10_000_000L;

    private HttpResponse<String> internal(String path, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) {
            b.header(SupportBridgeTokenFilter.HEADER, token);
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void serviceTokenIsRequiredAndUserTokensAreNotAccepted() throws Exception {
        assertEquals(401, internal("/internal/v1/orders/CMD-INCONNUE", null).statusCode());
        assertEquals(401, internal("/internal/v1/orders/CMD-INCONNUE", "mauvais-jeton").statusCode());
        assertEquals(404, internal("/internal/v1/orders/CMD-INCONNUE", TOKEN).statusCode());
        assertEquals(404, internal("/internal/v1/deliveries/pas-un-uuid", TOKEN).statusCode());
        assertEquals(404, internal("/internal/v1/sellers/boutique-inexistante", TOKEN).statusCode());
    }

    @Test
    void ordersOfANewUserAreEmptyAndLegacyTicketsAreExported() throws Exception {
        String phone = "+22893" + String.format("%07d", rand);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Client Pont Support", phone, "Passw0rd!", null), null, 201);
        String userId = registered.get("user").get("id").asText();
        String access = registered.get("accessToken").asText();

        HttpResponse<String> orders = internal("/internal/v1/users/" + userId + "/orders", TOKEN);
        assertEquals(200, orders.statusCode());
        assertEquals("[]", orders.body().replace(" ", ""));

        JsonNode ticket = post("/api/support-tickets", new CreateSupportTicketRequest(
                "Livraison", "Colis non reçu", "Le colis est marqué livré.", null, null), access, 201);
        String number = ticket.get("ticketNumber").asText();

        boolean found = false;
        for (int page = 0; page < 1000 && !found; page++) {
            HttpResponse<String> res = internal("/internal/v1/support-tickets?size=100&page=" + page, TOKEN);
            assertEquals(200, res.statusCode());
            JsonNode body = objectMapper.readTree(res.body());
            for (JsonNode item : body.get("items")) {
                if (number.equals(item.get("ticketNumber").asText())) {
                    assertEquals(userId, item.get("requesterId").asText());
                    assertEquals(phone, item.get("requesterPhone").asText());
                    assertEquals("Livraison", item.get("category").asText());
                    assertEquals("OPEN", item.get("status").asText());
                    found = true;
                }
            }
            if (body.get("last").asBoolean()) {
                break;
            }
        }
        assertTrue(found, "le ticket " + number + " doit être exporté");
    }
}
