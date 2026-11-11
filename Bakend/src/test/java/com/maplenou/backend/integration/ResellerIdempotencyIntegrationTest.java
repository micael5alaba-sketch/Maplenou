package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Idempotence et concurrence sur l'argent des partenaires : un meme evenement (double clic, renvoi apres
 * coupure reseau, requetes simultanees, rejeu d'une action admin) ne doit JAMAIS creer deux retraits,
 * deux reservations, deux paiements ni deux liberations de reserve. Verifie aussi le plafond de clics.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "app.rate-limit.reseller-click.max-per-minute=6")
class ResellerIdempotencyIntegrationTest extends AbstractIntegrationTest {

    private final long rand = System.nanoTime() % 100_000_000L;
    private final HttpClient http = HttpClient.newHttpClient();

    private String superToken, partnerToken;
    private StaffSession support, approver;
    private String partnerProfileId;

    private HttpResponse<String> raw(String method, String path, String body, String token, String idemKey) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (idemKey != null) b.header("Idempotency-Key", idemKey);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static final String WITHDRAW_BODY = "{\"amount\":3000,\"method\":\"MIX_BY_YAS\",\"paymentPhone\":\"+22890000001\"}";

    @Test
    @Order(1)
    void setup() throws Exception {
        superToken = login("+22890000000", "Micael2005@");
        support = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        approver = createStaffWithMfa(superToken, "ADMIN", false);
        JsonNode partner = post("/api/auth/register",
                new RegisterRequest("Idem Partner", "+22825" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        partnerToken = partner.get("accessToken").asText();
        partnerProfileId = post("/api/reseller/apply", Map.of("acceptTerms", true), partnerToken, 201).get("id").asText();
        patch("/api/admin/resellers/" + partnerProfileId + "/approve", null, support.token(), 200);
        post("/api/admin/resellers/" + partnerProfileId + "/adjustments",
                Map.of("amount", 20000, "reason", "Solde de test", "requestKey", "init-" + rand), superToken, 201);
        assertEquals(20000, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
    }

    @Test
    @Order(2)
    void sameKeyReturnsTheSameWithdrawalAndReservesOnce() throws Exception {
        String key = "wd-" + rand;
        HttpResponse<String> first = raw("POST", "/api/reseller/withdrawals", WITHDRAW_BODY, partnerToken, key);
        assertEquals(201, first.statusCode(), first.body());
        HttpResponse<String> replay = raw("POST", "/api/reseller/withdrawals", WITHDRAW_BODY, partnerToken, key);
        assertEquals(201, replay.statusCode(), "le rejeu avec la meme cle renvoie le meme retrait");
        assertEquals(objectMapper.readTree(first.body()).get("id").asText(), objectMapper.readTree(replay.body()).get("id").asText());
        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(3000, wallet.get("reserved").asLong(), "une seule reservation");
        assertEquals(17000, wallet.get("available").asLong());
        assertEquals(1, get("/api/reseller/withdrawals", partnerToken, 200).get("content").size());
        // Sans cle, une seconde demande pendant qu'une est en cours est refusee
        assertEquals(409, raw("POST", "/api/reseller/withdrawals", WITHDRAW_BODY, partnerToken, null).statusCode());
    }

    @Test
    @Order(3)
    void adminActionsReplayedDoNotDuplicateMoneyMovements() throws Exception {
        String wid = get("/api/reseller/withdrawals", partnerToken, 200).get("content").get(0).get("id").asText();
        patch("/api/admin/resellers/withdrawals/" + wid + "/prepare", null, support.token(), 200);
        assertEquals(409, raw("PATCH", "/api/admin/resellers/withdrawals/" + wid + "/prepare", null, support.token(), null).statusCode());
        patch("/api/admin/resellers/withdrawals/" + wid + "/submit", null, support.token(), 200);
        post("/api/admin/resellers/withdrawals/" + wid + "/approve", Map.of("mfaCode", approver.code()), approver.token(), 200);

        // Paiement lance deux fois en meme temps : un seul passe, frais pris une seule fois
        List<CompletableFuture<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            calls.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return raw("PATCH", "/api/admin/resellers/withdrawals/" + wid + "/paid", "{\"notes\":\"REF-" + rand + "\"}",
                            approver.token(), null).statusCode();
                } catch (Exception e) {
                    return -1;
                }
            }));
        }
        long ok = calls.stream().map(CompletableFuture::join).filter(c -> c == 200).count();
        assertEquals(1, ok, "un seul paiement doit reussir");
        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(0, wallet.get("reserved").asLong());
        assertEquals(3000, wallet.get("totalPaid").asLong());
        assertEquals(90, wallet.get("totalFees").asLong(), "frais preleves une seule fois");
        assertEquals(17000, wallet.get("available").asLong());
        // Un retrait paye ne peut plus etre rejete (pas de liberation de reserve apres paiement)
        assertEquals(409, raw("PATCH", "/api/admin/resellers/withdrawals/" + wid + "/reject", "{\"notes\":\"x\"}", approver.token(), null).statusCode());
        assertEquals(17000, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
    }

    @Test
    @Order(4)
    void simultaneousRequestsCreateOnlyOneWithdrawal() throws Exception {
        // 8 demandes simultanees avec des cles differentes : une seule peut etre ouverte
        List<CompletableFuture<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            String key = "par-" + rand + "-" + i;
            calls.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return raw("POST", "/api/reseller/withdrawals", WITHDRAW_BODY, partnerToken, key).statusCode();
                } catch (Exception e) {
                    return -1;
                }
            }));
        }
        List<Integer> codes = calls.stream().map(CompletableFuture::join).toList();
        assertEquals(1, codes.stream().filter(c -> c == 201).count(), "codes=" + codes);
        assertEquals(7, codes.stream().filter(c -> c == 409).count(), "codes=" + codes);
        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(3000, wallet.get("reserved").asLong());
        assertEquals(14000, wallet.get("available").asLong());

        // Rejet rejoue : la reserve n'est liberee qu'une fois
        String wid = get("/api/reseller/withdrawals", partnerToken, 200).get("content").get(0).get("id").asText();
        patch("/api/admin/resellers/withdrawals/" + wid + "/prepare", null, support.token(), 200);
        patch("/api/admin/resellers/withdrawals/" + wid + "/submit", null, support.token(), 200);
        patch("/api/admin/resellers/withdrawals/" + wid + "/reject", Map.of("notes", "test"), approver.token(), 200);
        assertEquals(409, raw("PATCH", "/api/admin/resellers/withdrawals/" + wid + "/reject", "{\"notes\":\"test\"}", approver.token(), null).statusCode());
        JsonNode after = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(0, after.get("reserved").asLong());
        assertEquals(17000, after.get("available").asLong());
    }

    @Test
    @Order(5)
    void adjustmentsAreIdempotentUnderConcurrency() throws Exception {
        String key = "conc-" + rand;
        List<CompletableFuture<Integer>> calls = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            calls.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return raw("POST", "/api/admin/resellers/" + partnerProfileId + "/adjustments",
                            "{\"amount\":1000,\"reason\":\"test\",\"requestKey\":\"" + key + "\"}", superToken, null).statusCode();
                } catch (Exception e) {
                    return -1;
                }
            }));
        }
        calls.forEach(c -> assertEquals(201, c.join()));
        assertEquals(18000, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong(), "credite une seule fois");
    }

    @Test
    @Order(6)
    void clickEndpointIsRateLimited() throws Exception {
        boolean limited = false;
        for (int i = 0; i < 20 && !limited; i++) {
            limited = raw("POST", "/api/reseller/clicks", "{\"code\":\"inconnu1\"}", null, null).statusCode() == 429;
        }
        assertTrue(limited, "la route publique des clics doit etre plafonnee");
    }
}
