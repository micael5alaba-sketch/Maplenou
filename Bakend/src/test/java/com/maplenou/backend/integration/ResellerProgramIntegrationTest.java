package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ShopStatus;
import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.courier.CourierDocumentStatus;
import com.maplenou.backend.courier.CourierDocumentType;
import com.maplenou.backend.courier.dto.ReviewCourierDocumentRequest;
import com.maplenou.backend.courier.dto.SubmitCourierDocumentRequest;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.delivery.ProofType;
import com.maplenou.backend.delivery.dto.AssignAgentRequest;
import com.maplenou.backend.delivery.dto.UpdateDeliveryRequest;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.order.dto.PaymentWebhookRequest;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Programme partenaire revendeur, de bout en bout : demande et approbation, catalogue éligible, lien,
 * clic et jeton d'attribution, commissions figées à la commande, crédit à la livraison seulement,
 * retrait (réserve, circuit de vérification/approbation avec MFA, frais au paiement), régularisation,
 * paramètres versionnés et droits.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.springframework.test.context.TestPropertySource(properties = "app.rate-limit.reseller-click.max-per-minute=100000")
class ResellerProgramIntegrationTest extends AbstractIntegrationTest {

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    private final long rand = System.nanoTime() % 100_000_000L;

    private String superToken, buyerToken, partnerToken, sellerToken, agentToken, outsiderToken;
    private StaffSession support, approver;
    private UUID agentId, shopId, productId, cheapProductId, variantId, addressId, partnerId, orderId, subOrderId;
    private String linkCode, clickToken;

    @Test
    @Order(1)
    void setup() throws Exception {
        superToken = login("+22890000000", "Micael2005@");
        support = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        approver = createStaffWithMfa(superToken, "ADMIN", false);

        JsonNode buyer = post("/api/auth/register", new RegisterRequest("Rs Buyer", "+22820" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        buyerToken = buyer.get("accessToken").asText();
        JsonNode partner = post("/api/auth/register", new RegisterRequest("Rs Partner", "+22821" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        partnerToken = partner.get("accessToken").asText();
        partnerId = UUID.fromString(partner.get("user").get("id").asText());
        JsonNode outsider = post("/api/auth/register", new RegisterRequest("Rs Outsider", "+22822" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        outsiderToken = outsider.get("accessToken").asText();
        JsonNode seller = post("/api/auth/register", new RegisterRequest("Rs Seller", "+22823" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        String sellerPhone = "+22823" + String.format("%07d", rand % 10_000_000L);
        JsonNode agent = post("/api/auth/register", new RegisterRequest("Rs Agent", "+22824" + String.format("%07d", rand % 10_000_000L), "Passw0rd!", null), null, 201);
        agentId = UUID.fromString(agent.get("user").get("id").asText());
        patch("/api/admin/users/" + agentId + "/role", new ChangeRoleRequest(Role.DELIVERY_AGENT), superToken, 200);
        agentToken = login("+22824" + String.format("%07d", rand % 10_000_000L), "Passw0rd!");
        for (CourierDocumentType type : CourierDocumentType.values()) {
            JsonNode doc = post("/api/delivery-agents/me/documents",
                    new SubmitCourierDocumentRequest(type, "https://res.cloudinary.com/demo/image/upload/v1/docs/d.jpg", null), agentToken, 201);
            patch("/api/admin/courier-documents/" + doc.get("id").asText() + "/review",
                    new ReviewCourierDocumentRequest(CourierDocumentStatus.VALID, null), superToken, 200);
        }

        JsonNode apply = post("/api/sellers/apply", new ApplySellerRequest("Boutique Rs " + rand, null), seller.get("accessToken").asText(), 201);
        patch("/api/sellers/admin/" + apply.get("id").asText() + "/status", new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), superToken, 200);
        sellerToken = login(sellerPhone, "Passw0rd!");
        JsonNode shop = post("/api/shops", new CreateShopRequest("Boutique Rs " + rand, "d", "Lomé", "Centre", null, null, null), sellerToken, 201);
        shopId = UUID.fromString(shop.get("id").asText());
        patch("/api/admin/shops/" + shopId + "/status", new UpdateShopStatusRequest(ShopStatus.APPROVED), superToken, 200);
        JsonNode category = post("/api/categories", new CreateCategoryRequest("Cat Rs " + rand, null, null, null), superToken, 201);
        UUID categoryId = UUID.fromString(category.get("id").asText());

        // Produit éligible : base 10 000 -> prix affiché 10 500 (> seuil : taux 3 %)
        JsonNode product = post("/api/shops/mine/products",
                new CreateProductRequest("Produit Rs " + rand, "desc", new BigDecimal("10000.00"), null, categoryId,
                        List.of(new CreateVariantRequest("Standard", null, 20, "SKU-RS-" + rand)), null, null, null, null), sellerToken, 201);
        productId = UUID.fromString(product.get("id").asText());
        variantId = UUID.fromString(product.get("variants").get(0).get("id").asText());
        patch("/api/shops/mine/products/" + productId,
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null, null, null, null), sellerToken, 200);

        // Produit non éligible : base 2 000 -> prix affiché 2 100 (< 2 500)
        JsonNode cheap = post("/api/shops/mine/products",
                new CreateProductRequest("Produit Rs pas cher " + rand, "desc", new BigDecimal("2000.00"), null, categoryId,
                        List.of(new CreateVariantRequest("Standard", null, 20, "SKU-RSC-" + rand)), null, null, null, null), sellerToken, 201);
        cheapProductId = UUID.fromString(cheap.get("id").asText());
        patch("/api/shops/mine/products/" + cheapProductId,
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null, null, null, null), sellerToken, 200);

        JsonNode address = post("/api/users/me/addresses", new CreateAddressRequest("Maison", "Lomé", "Bè", "detail", 6.13, 1.22, true), buyerToken, 201);
        addressId = UUID.fromString(address.get("id").asText());
    }

    @Test
    @Order(2)
    void nonPartnerCannotUsePartnerFeatures() throws Exception {
        get("/api/reseller/catalog", outsiderToken, 403);
        post("/api/reseller/links", Map.of("productId", productId.toString()), outsiderToken, 403);
        get("/api/reseller/me", outsiderToken, 404);
        // Les paramètres financiers sont lisibles mais pas modifiables par un compte ordinaire
        get("/api/reseller/settings", outsiderToken, 200);
        get("/api/admin/resellers", outsiderToken, 403);
    }

    @Test
    @Order(3)
    void applicationAndAdminDecision() throws Exception {
        post("/api/reseller/apply", Map.of("acceptTerms", false), partnerToken, 400);
        JsonNode profile = post("/api/reseller/apply", Map.of("acceptTerms", true), partnerToken, 201);
        assertEquals("PENDING", profile.get("status").asText());
        assertEquals("2.0", profile.get("termsVersion").asText());
        post("/api/reseller/apply", Map.of("acceptTerms", true), partnerToken, 409);

        UUID id = UUID.fromString(profile.get("id").asText());
        // En attente : pas d'accès au catalogue
        get("/api/reseller/catalog", partnerToken, 403);
        // Un compte ordinaire ne décide pas
        patch("/api/admin/resellers/" + id + "/approve", null, outsiderToken, 403);
        // Refus sans motif : 400
        patch("/api/admin/resellers/" + id + "/reject", Map.of(), support.token(), 400);
        JsonNode approved = patch("/api/admin/resellers/" + id + "/approve", null, support.token(), 200);
        assertEquals("APPROVED", approved.get("status").asText());
        // Une fois décidée, la demande ne peut plus être approuvée à nouveau
        patch("/api/admin/resellers/" + id + "/approve", null, support.token(), 409);
        // Le support lit mais ne modifie pas les paramètres financiers
        get("/api/admin/resellers/settings", support.token(), 200);
        put("/api/admin/resellers/settings", Map.ofEntries(
                Map.entry("shareRate", 0.30), Map.entry("minUnitPrice", 2500), Map.entry("tierThreshold", 10000),
                Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03), Map.entry("attributionDays", 30),
                Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03), Map.entry("programActive", true),
                Map.entry("reason", "tentative support")), support.token(), 403);
    }

    @Test
    @Order(4)
    void catalogShowsOnlyEligibleProductsAndLinkIsUnique() throws Exception {
        JsonNode catalog = get("/api/reseller/catalog?size=50", partnerToken, 200);
        boolean foundEligible = false;
        for (JsonNode item : catalog.get("content")) {
            String pid = item.get("productId").asText();
            assertNotEquals(cheapProductId.toString(), pid, "un produit < 2 500 FCFA ne doit pas apparaître");
            if (pid.equals(productId.toString())) {
                foundEligible = true;
                assertEquals(10500, item.get("unitPrice").asLong());
                assertEquals(79, item.get("partnerShare").asLong()); // 3 % de 10 500 = 315 ; 25 % = 78,75 -> 79
            }
        }
        // Le catalogue est paginé et trié du plus récent : le produit créé ici est en tête.
        assertTrue(foundEligible || catalog.get("content").size() >= 50);

        post("/api/reseller/links", Map.of("productId", cheapProductId.toString()), partnerToken, 409);
        JsonNode link = post("/api/reseller/links", Map.of("productId", productId.toString()), partnerToken, 201);
        linkCode = link.get("code").asText();
        JsonNode again = post("/api/reseller/links", Map.of("productId", productId.toString()), partnerToken, 201);
        assertEquals(linkCode, again.get("code").asText(), "un seul lien par partenaire et par produit");
    }

    @Test
    @Order(5)
    void clickRecordsAttributionToken() throws Exception {
        post("/api/reseller/clicks", Map.of("code", "inconnu1"), null, 404);
        // Le partenaire qui clique sur son propre lien ne génère aucune attribution valide
        JsonNode self = post("/api/reseller/clicks", Map.of("code", linkCode, "visitorId", "v-self"), partnerToken, 200);
        assertFalse(self.get("valid").asBoolean());
        // Redirection publique du lien
        exchange("GET", "/r/" + linkCode, null, null, 302);

        JsonNode click = post("/api/reseller/clicks", Map.of("code", linkCode, "visitorId", "v-buyer-" + rand), buyerToken, 200);
        assertTrue(click.get("valid").asBoolean());
        clickToken = click.get("token").asText();
        assertNotNull(clickToken);
        // Reclic rapide : toujours valide (le dernier clic l'emporte)
        JsonNode again = post("/api/reseller/clicks", Map.of("code", linkCode, "visitorId", "v-buyer-" + rand), buyerToken, 200);
        assertTrue(again.get("valid").asBoolean());
        clickToken = again.get("token").asText();
    }

    @Test
    @Order(6)
    void orderFreezesCommissionsPerUnitButCreditsNothingYet() throws Exception {
        post("/api/cart/items", new AddToCartRequest(variantId, 2), buyerToken, 200);
        // Un jeton falsifié est ignoré sans faire échouer la commande
        JsonNode order = post("/api/orders",
                new PlaceOrderRequest(addressId, null, null, null, List.of(clickToken, "faux.jeton")), buyerToken, 201);
        orderId = UUID.fromString(order.get("id").asText());
        subOrderId = UUID.fromString(order.get("subOrders").get(0).get("id").asText());

        JsonNode commissions = get("/api/reseller/commissions", partnerToken, 200);
        assertEquals(2, commissions.get("content").size(), "une commission par unité");
        for (JsonNode c : commissions.get("content")) {
            assertEquals("PENDING", c.get("status").asText());
            assertEquals(10500, c.get("unitPrice").asLong());
            assertEquals(315, c.get("maplenouCommission").asLong());
            assertEquals(79, c.get("partnerShare").asLong());
            assertFalse(c.has("commissionRate"), "le taux de commission Maplenou n est jamais montre au partenaire");
            assertFalse(c.has("maplenouShare"));
        }
        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(0, wallet.get("available").asLong());
        assertEquals(158, wallet.get("pending").asLong());
    }

    @Test
    @Order(7)
    void paymentAloneDoesNotCreditButDeliveryDoes() throws Exception {
        postWithHeader("/api/webhooks/payment/confirmed", new PaymentWebhookRequest(orderId.toString(), "PAY-RS-" + rand),
                "X-Webhook-Secret", webhookSecret, 200);
        assertEquals(0, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong(),
                "le paiement seul ne crédite pas le partenaire");

        patch("/api/sub-orders/" + subOrderId + "/status", new UpdateSubOrderStatusRequest(SubOrderStatus.PREPARING), sellerToken, 200);
        patch("/api/sub-orders/" + subOrderId + "/status", new UpdateSubOrderStatusRequest(SubOrderStatus.READY_FOR_PICKUP), sellerToken, 200);
        JsonNode delivery = post("/api/deliveries/sub-orders/" + subOrderId, null, superToken, 201);
        String deliveryId = delivery.get("id").asText();
        patch("/api/deliveries/" + deliveryId + "/assign", new AssignAgentRequest(agentId), superToken, 200);
        patch("/api/deliveries/" + deliveryId + "/status", new UpdateDeliveryRequest(DeliveryStatus.IN_TRANSIT, null, null, null), agentToken, 200);
        assertEquals(0, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
        patch("/api/deliveries/" + deliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.DELIVERED, ProofType.PHOTO,
                        "https://res.cloudinary.com/demo/image/upload/v1/proofs/p.jpg", "ok"), agentToken, 200);

        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(158, wallet.get("available").asLong());
        assertEquals(0, wallet.get("pending").asLong());
        assertEquals(158, wallet.get("totalEarned").asLong());
        for (JsonNode c : get("/api/reseller/commissions", partnerToken, 200).get("content")) {
            assertEquals("CREDITED", c.get("status").asText());
        }
        JsonNode tx = get("/api/reseller/wallet/transactions", partnerToken, 200);
        assertEquals(2, tx.get("content").size());
    }

    @Test
    @Order(8)
    void changingSettingsDoesNotRewritePastCommissions() throws Exception {
        JsonNode current = get("/api/admin/resellers/settings", superToken, 200);
        int version = current.get("version").asInt();
        // Motif obligatoire
        put("/api/admin/resellers/settings", Map.ofEntries(
                Map.entry("shareRate", 0.30), Map.entry("minUnitPrice", 2500), Map.entry("tierThreshold", 10000),
                Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03), Map.entry("attributionDays", 30),
                Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03), Map.entry("programActive", true),
                Map.entry("reason", "")), superToken, 400);
        JsonNode updated = put("/api/admin/resellers/settings", Map.ofEntries(
                Map.entry("shareRate", 0.30), Map.entry("minUnitPrice", 2500), Map.entry("tierThreshold", 10000),
                Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03), Map.entry("attributionDays", 30),
                Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03), Map.entry("programActive", true),
                Map.entry("reason", "Test de versionnement")), superToken, 200);
        assertEquals(version + 1, updated.get("version").asInt());
        // Les commissions existantes gardent leur version et leurs montants
        int checked = 0;
        for (JsonNode c : get("/api/admin/resellers/commissions?size=100", superToken, 200).get("content")) {
            if (partnerProfileId().equals(c.get("partnerId").asText())) {
                assertEquals(79, c.get("partnerShare").asLong());
                assertEquals(version, c.get("settingsVersion").asInt());
                checked++;
            }
        }
        assertEquals(2, checked);
        // Retour aux valeurs d'origine (nouvelle version), l'historique conserve tout
        put("/api/admin/resellers/settings", Map.ofEntries(
                Map.entry("shareRate", 0.25), Map.entry("minUnitPrice", 2500), Map.entry("tierThreshold", 10000),
                Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03), Map.entry("attributionDays", 30),
                Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03), Map.entry("programActive", true),
                Map.entry("reason", "Retour aux valeurs initiales")), superToken, 200);
        assertTrue(get("/api/admin/resellers/settings/history", superToken, 200).get("content").size() >= 3);
    }

    @Test
    @Order(9)
    void withdrawalRulesReserveCircuitAndFees() throws Exception {
        // Sous le minimum
        post("/api/reseller/withdrawals", Map.of("amount", 158, "method", "MIX_BY_YAS", "paymentPhone", "+22890000001"), partnerToken, 400);
        // Solde insuffisant
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000001"), partnerToken, 409);

        // Régularisation positive par le Super Admin (le support n'y a pas droit)
        String key = "k-" + rand;
        post("/api/admin/resellers/" + partnerProfileId() + "/adjustments",
                Map.of("amount", 5000, "reason", "Bonus de test", "requestKey", key), support.token(), 403);
        post("/api/admin/resellers/" + partnerProfileId() + "/adjustments",
                Map.of("amount", 5000, "reason", "", "requestKey", key), superToken, 400);
        post("/api/admin/resellers/" + partnerProfileId() + "/adjustments",
                Map.of("amount", 5000, "reason", "Bonus de test", "requestKey", key), superToken, 201);
        // Rejouer la même clé n'écrit pas deux fois
        post("/api/admin/resellers/" + partnerProfileId() + "/adjustments",
                Map.of("amount", 5000, "reason", "Bonus de test", "requestKey", key), superToken, 201);
        assertEquals(5158, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());

        JsonNode w = post("/api/reseller/withdrawals",
                Map.of("amount", 3000, "method", "MOOV_AFRICA", "paymentPhone", "+22890000001"), partnerToken, 201);
        String wid = w.get("id").asText();
        assertEquals("PENDING", w.get("status").asText());
        assertEquals(90, w.get("feeAmount").asLong());
        assertEquals(2910, w.get("netAmount").asLong());
        // Une seule demande en cours
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MOOV_AFRICA", "paymentPhone", "+22890000001"), partnerToken, 409);
        JsonNode reserved = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(2158, reserved.get("available").asLong());
        assertEquals(3000, reserved.get("reserved").asLong());

        // Circuit : le support prépare et soumet, ne peut pas approuver
        patch("/api/admin/resellers/withdrawals/" + wid + "/prepare", null, support.token(), 200);
        patch("/api/admin/resellers/withdrawals/" + wid + "/submit", null, support.token(), 200);
        post("/api/admin/resellers/withdrawals/" + wid + "/approve", Map.of("mfaCode", support.code()), support.token(), 403);
        // Approbation : MFA obligatoire
        post("/api/admin/resellers/withdrawals/" + wid + "/approve", Map.of("mfaCode", "000000"), approver.token(), 401);
        JsonNode approvedW = post("/api/admin/resellers/withdrawals/" + wid + "/approve", Map.of("mfaCode", approver.code()), approver.token(), 200);
        assertEquals("APPROVED", approvedW.get("status").asText());
        // Paiement : les frais ne sont pris qu'ici
        assertEquals(3000, get("/api/reseller/wallet", partnerToken, 200).get("reserved").asLong());
        JsonNode paid = patch("/api/admin/resellers/withdrawals/" + wid + "/paid", Map.of("notes", "MOOV-REF-" + rand), approver.token(), 200);
        assertEquals("PAID", paid.get("status").asText());
        // scenario 22 : reference de paiement et frais enregistres
        assertEquals("MOOV-REF-" + rand, paid.get("paymentReference").asText());
        assertEquals(90, paid.get("feeAmount").asLong());
        JsonNode after = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(0, after.get("reserved").asLong());
        assertEquals(2158, after.get("available").asLong());
        assertEquals(3000, after.get("totalPaid").asLong());
        assertEquals(90, after.get("totalFees").asLong());
        // Historique complet du retrait
        assertTrue(get("/api/admin/resellers/withdrawals/" + wid + "/history", superToken, 200).size() >= 4);
    }

    @Test
    @Order(10)
    void rejectedWithdrawalReleasesReserve() throws Exception {
        post("/api/admin/resellers/" + partnerProfileId() + "/adjustments",
                Map.of("amount", 2000, "reason", "Complément de test", "requestKey", "k2-" + rand), superToken, 201);
        JsonNode w = post("/api/reseller/withdrawals",
                Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000001"), partnerToken, 201);
        String wid = w.get("id").asText();
        patch("/api/admin/resellers/withdrawals/" + wid + "/prepare", null, support.token(), 200);
        patch("/api/admin/resellers/withdrawals/" + wid + "/submit", null, support.token(), 200);
        patch("/api/admin/resellers/withdrawals/" + wid + "/reject", Map.of(), approver.token(), 400);
        JsonNode rejected = patch("/api/admin/resellers/withdrawals/" + wid + "/reject", Map.of("notes", "Numéro invalide"), approver.token(), 200);
        assertEquals("REJECTED", rejected.get("status").asText());
        JsonNode wallet = get("/api/reseller/wallet", partnerToken, 200);
        assertEquals(0, wallet.get("reserved").asLong());
        assertEquals(4158, wallet.get("available").asLong());
    }

    @Test
    @Order(11)
    void suspendedPartnerLosesAccessAndReactivationRestoresIt() throws Exception {
        String id = partnerProfileId();
        patch("/api/admin/resellers/" + id + "/suspend", Map.of(), support.token(), 400);
        patch("/api/admin/resellers/" + id + "/suspend", Map.of("reason", "Vérification"), support.token(), 200);
        get("/api/reseller/catalog", partnerToken, 403);
        post("/api/reseller/links", Map.of("productId", productId.toString()), partnerToken, 403);
        post("/api/reseller/withdrawals", Map.of("amount", 3000, "method", "MIX_BY_YAS", "paymentPhone", "+22890000001"), partnerToken, 403);
        // Le portefeuille reste consultable
        assertEquals(4158, get("/api/reseller/wallet", partnerToken, 200).get("available").asLong());
        // Un clic sur le lien d'un partenaire suspendu n'est pas valide
        assertFalse(post("/api/reseller/clicks", Map.of("code", linkCode, "visitorId", "v-x-" + rand), null, 200).get("valid").asBoolean());
        patch("/api/admin/resellers/" + id + "/reactivate", Map.of("reason", "Contrôle terminé"), support.token(), 200);
        get("/api/reseller/catalog", partnerToken, 200);
    }

    @Test
    @Order(12)
    void excludedProductAndAuditTrail() throws Exception {
        patch("/api/admin/resellers/products/" + productId + "/exclusion?excluded=true", Map.of("reason", "Test exclusion"), support.token(), 200);
        post("/api/reseller/links", Map.of("productId", productId.toString()), partnerToken, 409); // un produit exclu ne peut plus recevoir de lien
        // Le lien existant n'est plus valide pour de nouveaux clics
        assertFalse(post("/api/reseller/clicks", Map.of("code", linkCode, "visitorId", "v-y-" + rand), null, 200).get("valid").asBoolean());
        patch("/api/admin/resellers/products/" + productId + "/exclusion?excluded=false", Map.of("reason", "Fin test"), support.token(), 200);
        assertTrue(get("/api/admin/resellers/audit", superToken, 200).get("content").size() > 0);
        assertTrue(get("/api/admin/resellers/stats", superToken, 200).get("activePartners").asLong() >= 1);
    }

    private String cachedProfileId;

    private String partnerProfileId() throws Exception {
        if (cachedProfileId == null) {
            cachedProfileId = get("/api/reseller/me", partnerToken, 200).get("id").asText();
        }
        return cachedProfileId;
    }
}
