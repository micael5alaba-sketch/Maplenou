package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.*;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Controle d'acces du programme partenaire (section "tests de securite" du document technique) : routes
 * protegees sans session, cloisonnement entre partenaires, droits de chaque role du personnel, parametres
 * financiers reserves au Super Admin, parametres invalides refuses, donnees client jamais exposees.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "app.rate-limit.reseller-click.max-per-minute=100000")
class ResellerAccessControlIntegrationTest extends AbstractIntegrationTest {

    private final long rand = System.nanoTime() % 100_000_000L;
    private String superToken, partnerToken, otherPartnerToken, plainToken;
    private StaffSession support, auditor, deliveryAdmin;
    private String partnerId, otherPartnerId, withdrawalId;

    private static Map<String, Object> settingsBody(String reason, Object shareRate) {
        return Map.ofEntries(Map.entry("shareRate", shareRate), Map.entry("minUnitPrice", 2500),
                Map.entry("tierThreshold", 10000), Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03),
                Map.entry("attributionDays", 30), Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03),
                Map.entry("programActive", true), Map.entry("reason", reason));
    }

    @Test
    @Order(1)
    void setup() throws Exception {
        superToken = login("+22890000000", "Micael2005@");
        support = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        auditor = createStaffWithMfa(superToken, "AUDITOR", false);
        deliveryAdmin = createStaffWithMfa(superToken, "DELIVERY_ADMIN", false);
        JsonNode p1 = post("/api/auth/register", new RegisterRequest("Ac Partner 1", "+22840" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        JsonNode p2 = post("/api/auth/register", new RegisterRequest("Ac Partner 2", "+22841" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        plainToken = post("/api/auth/register", new RegisterRequest("Ac Plain", "+22842" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201).get("accessToken").asText();
        partnerToken = p1.get("accessToken").asText();
        otherPartnerToken = p2.get("accessToken").asText();
        partnerId = post("/api/reseller/apply", Map.of("acceptTerms", true), partnerToken, 201).get("id").asText();
        otherPartnerId = post("/api/reseller/apply", Map.of("acceptTerms", true), otherPartnerToken, 201).get("id").asText();
        patch("/api/admin/resellers/" + partnerId + "/approve", null, support.token(), 200);
        patch("/api/admin/resellers/" + otherPartnerId + "/approve", null, support.token(), 200);
        post("/api/admin/resellers/" + partnerId + "/adjustments",
                Map.of("amount", 10000, "reason", "Solde de test", "requestKey", "ac-" + rand), superToken, 201);
        withdrawalId = post("/api/reseller/withdrawals",
                Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000002"), partnerToken, 201).get("id").asText();
    }

    @Test
    @Order(2)
    void anonymousRequestsAreRejectedWith401() throws Exception {
        for (String path : new String[]{"/api/reseller/me", "/api/reseller/catalog", "/api/reseller/links",
                "/api/reseller/commissions", "/api/reseller/wallet", "/api/reseller/wallet/transactions",
                "/api/reseller/withdrawals", "/api/reseller/stats?period=7"}) {
            get(path, null, 401);
        }
        post("/api/reseller/apply", Map.of("acceptTerms", true), null, 401);
        post("/api/reseller/links", Map.of("productId", UUID.randomUUID().toString()), null, 401);
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000002"), null, 401);
        get("/api/admin/resellers", null, 401);
        get("/api/admin/resellers/settings", null, 401);
        // Seules routes publiques : lien partage (redirection) et clic
        exchange("GET", "/r/inconnu1", null, null, 302);
        post("/api/reseller/clicks", Map.of("code", "inconnu1"), null, 404);
    }

    @Test
    @Order(3)
    void partnersAreIsolatedFromEachOther() throws Exception {
        // Le portefeuille et les retraits ne sont jamais ceux d'un autre partenaire
        assertEquals(0, get("/api/reseller/wallet", otherPartnerToken, 200).get("available").asLong());
        assertEquals(0, get("/api/reseller/withdrawals", otherPartnerToken, 200).get("content").size());
        assertEquals(1, get("/api/reseller/withdrawals", partnerToken, 200).get("content").size());
        assertEquals(0, get("/api/reseller/wallet/transactions", otherPartnerToken, 200).get("content").size());
        // Un compte ordinaire n'a aucune donnee partenaire
        get("/api/reseller/wallet", plainToken, 403);
        get("/api/reseller/withdrawals", plainToken, 403);
        get("/api/reseller/commissions", plainToken, 403);
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000002"), plainToken, 403);
        // Un partenaire ne peut ni agir sur le circuit admin ni lire les donnees de l'administration
        for (String path : new String[]{"/api/admin/resellers", "/api/admin/resellers/commissions",
                "/api/admin/resellers/withdrawals", "/api/admin/resellers/audit", "/api/admin/resellers/stats",
                "/api/admin/resellers/" + otherPartnerId + "/wallet"}) {
            get(path, partnerToken, 403);
        }
        patch("/api/admin/resellers/" + otherPartnerId + "/suspend", Map.of("reason", "x"), partnerToken, 403);
        post("/api/admin/resellers/" + partnerId + "/adjustments",
                Map.of("amount", 999999, "reason", "Auto-credit", "requestKey", "evil"), partnerToken, 403);
        post("/api/admin/resellers/withdrawals/" + withdrawalId + "/approve", Map.of("mfaCode", "123456"), partnerToken, 403);
        patch("/api/admin/resellers/withdrawals/" + withdrawalId + "/paid", Map.of(), partnerToken, 403);
        assertEquals(7000, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
    }

    @Test
    @Order(4)
    void staffRolesHaveOnlyTheirRights() throws Exception {
        // Auditeur : lecture seule
        get("/api/admin/resellers", auditor.token(), 200);
        get("/api/admin/resellers/commissions", auditor.token(), 200);
        get("/api/admin/resellers/audit", auditor.token(), 200);
        patch("/api/admin/resellers/" + otherPartnerId + "/suspend", Map.of("reason", "x"), auditor.token(), 403);
        patch("/api/admin/resellers/" + otherPartnerId + "/approve", null, auditor.token(), 403);
        post("/api/admin/resellers/" + partnerId + "/adjustments",
                Map.of("amount", 1, "reason", "x", "requestKey", "aud"), auditor.token(), 403);
        patch("/api/admin/resellers/withdrawals/" + withdrawalId + "/prepare", null, auditor.token(), 403);
        // Administrateur des livraisons : aucun acces au programme
        get("/api/admin/resellers", deliveryAdmin.token(), 403);
        get("/api/admin/resellers/commissions", deliveryAdmin.token(), 403);
        get("/api/admin/resellers/withdrawals", deliveryAdmin.token(), 403);
        patch("/api/admin/resellers/" + otherPartnerId + "/suspend", Map.of("reason", "x"), deliveryAdmin.token(), 403);
        // Agent support : gere les demandes, prepare les retraits, mais ni parametres, ni regularisation, ni approbation
        get("/api/admin/resellers", support.token(), 200);
        put("/api/admin/resellers/settings", settingsBody("tentative", 0.40), support.token(), 403);
        post("/api/admin/resellers/" + partnerId + "/adjustments",
                Map.of("amount", 1, "reason", "x", "requestKey", "sup"), support.token(), 403);
        post("/api/admin/resellers/withdrawals/" + withdrawalId + "/approve", Map.of("mfaCode", support.code()), support.token(), 403);
        patch("/api/admin/resellers/withdrawals/" + withdrawalId + "/paid", Map.of(), support.token(), 403);
        patch("/api/admin/resellers/withdrawals/" + withdrawalId + "/prepare", null, support.token(), 200);
        // Rien n'a bouge
        assertEquals(7000, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
    }

    @Test
    @Order(5)
    void invalidFinancialSettingsAreRefused() throws Exception {
        // Part trop elevee, taux incoherents, valeurs absurdes, motif absent
        put("/api/admin/resellers/settings", settingsBody("trop", 0.90), superToken, 400);
        put("/api/admin/resellers/settings", Map.ofEntries(Map.entry("shareRate", 0.25), Map.entry("minUnitPrice", 2500),
                Map.entry("tierThreshold", 10000), Map.entry("rateLow", 0.03), Map.entry("rateHigh", 0.05),
                Map.entry("attributionDays", 30), Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03),
                Map.entry("programActive", true), Map.entry("reason", "inverse")), superToken, 400);
        put("/api/admin/resellers/settings", Map.ofEntries(Map.entry("shareRate", 0.25), Map.entry("minUnitPrice", 5000),
                Map.entry("tierThreshold", 1000), Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03),
                Map.entry("attributionDays", 30), Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03),
                Map.entry("programActive", true), Map.entry("reason", "seuil sous le minimum")), superToken, 400);
        put("/api/admin/resellers/settings", settingsBody("", 0.25), superToken, 400);
        // Montants de retrait invalides
        post("/api/reseller/withdrawals", Map.of("amount", -5000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000002"), partnerToken, 400);
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "VIREMENT", "paymentPhone", "+22890000002"), partnerToken, 400);
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "abc"), partnerToken, 400);
    }

    @Test
    @Order(6)
    void apiNeverExposesCustomerData() throws Exception {
        JsonNode catalog = get("/api/reseller/catalog?size=5", partnerToken, 200);
        String body = catalog.toString().toLowerCase() + get("/api/reseller/commissions", partnerToken, 200).toString().toLowerCase();
        for (String forbidden : new String[]{"buyer", "phonenumber", "address", "customer", "commissionrate"}) {
            assertFalse(body.contains(forbidden), "champ interdit expose : " + forbidden);
        }
        // Le circuit admin refuse une action sans identifiant valide
        get("/api/admin/resellers/" + UUID.randomUUID(), superToken, 404);
        get("/api/admin/resellers/withdrawals/" + UUID.randomUUID(), superToken, 404);
    }
}
