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
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.order.dto.PaymentWebhookRequest;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest;
import com.maplenou.backend.promo.PromoScopeType;
import com.maplenou.backend.promo.dto.CreatePromoCodeRequest;
import com.maplenou.backend.reseller.ResellerAttributionService;
import com.maplenou.backend.returns.ProductCondition;
import com.maplenou.backend.returns.ReturnDecision;
import com.maplenou.backend.returns.dto.CreateReturnRequest;
import com.maplenou.backend.returns.dto.DecideReturnRequest;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Revue des 34 scenarios d'acceptation du document "Programme Partenaire Revendeur - specifications
 * techniques v2.0" (section 23). Chaque test indique les numeros de scenarios qu'il verifie ; les scenarios
 * 1, 2, 12 (partiel), 19 a 22, 25, 27, 30, 31 et 33 sont aussi verifies dans ResellerProgramIntegrationTest,
 * 17 et 26 dans ResellerIdempotencyIntegrationTest.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@org.springframework.test.context.TestPropertySource(properties = "app.rate-limit.reseller-click.max-per-minute=100000")
class ResellerScenariosIntegrationTest extends AbstractIntegrationTest {

    @Value("${app.webhook.secret}")
    private String webhookSecret;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SubOrderRepository subOrderRepository;
    @Autowired private ResellerAttributionService attributionService;
    @Autowired private PlatformTransactionManager txManager;

    private final long rand = System.nanoTime() % 100_000_000L;
    private String superToken, buyerToken, partnerAToken, partnerBToken, sellerToken, agentToken;
    private StaffSession support;
    private UUID agentId, shopId, categoryId, addressId;
    private String profileA, profileB;
    private final Map<String, UUID> productIds = new HashMap<>();
    private final Map<String, UUID> variantIds = new HashMap<>();
    private final Map<String, String> codesA = new HashMap<>();
    private final Map<String, String> codesB = new HashMap<>();
    private JsonNode deliveredOrder;

    private String phone(String prefix) {
        return "+22" + prefix + String.format("%07d", rand % 10_000_000L);
    }

    private UUID createProduct(String key, String base) throws Exception {
        JsonNode product = post("/api/shops/mine/products",
                new CreateProductRequest("Sc " + key + " " + rand, "desc", new BigDecimal(base), null, categoryId,
                        List.of(new CreateVariantRequest("Standard", null, 100, "SKU-SC-" + key + "-" + rand)), null, null, null, null),
                sellerToken, 201);
        UUID id = UUID.fromString(product.get("id").asText());
        productIds.put(key, id);
        variantIds.put(key, UUID.fromString(product.get("variants").get(0).get("id").asText()));
        patch("/api/shops/mine/products/" + id,
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null, null, null, null), sellerToken, 200);
        return id;
    }

    private String link(String partnerToken, String key) throws Exception {
        return post("/api/reseller/links", Map.of("productId", productIds.get(key).toString()), partnerToken, 201).get("code").asText();
    }

    private String click(String code, String visitor, String token) throws Exception {
        JsonNode r = post("/api/reseller/clicks", Map.of("code", code, "visitorId", visitor), token, 200);
        return r.get("valid").asBoolean() ? r.get("token").asText() : null;
    }

    private JsonNode order(String key, int qty, List<String> tokens, String promo) throws Exception {
        return orderMulti(Map.of(key, qty), tokens, promo);
    }

    private JsonNode orderMulti(Map<String, Integer> lines, List<String> tokens, String promo) throws Exception {
        for (var e : lines.entrySet()) {
            post("/api/cart/items", new AddToCartRequest(variantIds.get(e.getKey()), e.getValue()), buyerToken, 200);
        }
        return post("/api/orders", new PlaceOrderRequest(addressId, promo, null, null, tokens), buyerToken, 201);
    }

    private void pay(JsonNode order) throws Exception {
        postWithHeader("/api/webhooks/payment/confirmed", new PaymentWebhookRequest(order.get("id").asText(), "PAY-SC-" + System.nanoTime()),
                "X-Webhook-Secret", webhookSecret, 200);
    }

    private void deliver(JsonNode order) throws Exception {
        pay(order);
        for (JsonNode so : order.get("subOrders")) {
            String id = so.get("id").asText();
            patch("/api/sub-orders/" + id + "/status", new UpdateSubOrderStatusRequest(SubOrderStatus.PREPARING), sellerToken, 200);
            patch("/api/sub-orders/" + id + "/status", new UpdateSubOrderStatusRequest(SubOrderStatus.READY_FOR_PICKUP), sellerToken, 200);
            String deliveryId = post("/api/deliveries/sub-orders/" + id, null, superToken, 201).get("id").asText();
            patch("/api/deliveries/" + deliveryId + "/assign", new AssignAgentRequest(agentId), superToken, 200);
            patch("/api/deliveries/" + deliveryId + "/status", new UpdateDeliveryRequest(DeliveryStatus.IN_TRANSIT, null, null, null), agentToken, 200);
            patch("/api/deliveries/" + deliveryId + "/status",
                    new UpdateDeliveryRequest(DeliveryStatus.DELIVERED, ProofType.PHOTO,
                            "https://res.cloudinary.com/demo/image/upload/v1/proofs/p.jpg", "ok"), agentToken, 200);
        }
    }

    /** Commissions du partenaire pour un produit donne (le nom du produit contient la cle). */
    private List<JsonNode> commissions(String partnerToken, String key) throws Exception {
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode c : get("/api/reseller/commissions?size=100", partnerToken, 200).get("content")) {
            if (c.get("productName").asText().startsWith("Sc " + key + " ")) out.add(c);
        }
        return out;
    }

    private long available(String partnerToken) throws Exception {
        return get("/api/reseller/wallet", partnerToken, 200).get("available").asLong();
    }

    private Map<String, Object> settings(boolean active, String reason) {
        return Map.ofEntries(Map.entry("shareRate", 0.25), Map.entry("minUnitPrice", 2500), Map.entry("tierThreshold", 10000),
                Map.entry("rateLow", 0.05), Map.entry("rateHigh", 0.03), Map.entry("attributionDays", 30),
                Map.entry("minWithdrawal", 3000), Map.entry("withdrawalFeeRate", 0.03), Map.entry("programActive", active),
                Map.entry("reason", reason));
    }

    @Test
    @Order(1)
    void setup() throws Exception {
        superToken = login("+22890000000", "Micael2005@");
        support = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        buyerToken = post("/api/auth/register", new RegisterRequest("Sc Buyer", phone("830"), "Passw0rd!", null), null, 201).get("accessToken").asText();
        JsonNode a = post("/api/auth/register", new RegisterRequest("Sc PartnerA", phone("831"), "Passw0rd!", null), null, 201);
        JsonNode b = post("/api/auth/register", new RegisterRequest("Sc PartnerB", phone("832"), "Passw0rd!", null), null, 201);
        partnerAToken = a.get("accessToken").asText();
        partnerBToken = b.get("accessToken").asText();
        JsonNode seller = post("/api/auth/register", new RegisterRequest("Sc Seller", phone("833"), "Passw0rd!", null), null, 201);
        JsonNode agent = post("/api/auth/register", new RegisterRequest("Sc Agent", phone("834"), "Passw0rd!", null), null, 201);
        agentId = UUID.fromString(agent.get("user").get("id").asText());
        patch("/api/admin/users/" + agentId + "/role", new ChangeRoleRequest(Role.DELIVERY_AGENT), superToken, 200);
        agentToken = login(phone("834"), "Passw0rd!");
        for (CourierDocumentType type : CourierDocumentType.values()) {
            JsonNode doc = post("/api/delivery-agents/me/documents",
                    new SubmitCourierDocumentRequest(type, "https://res.cloudinary.com/demo/image/upload/v1/docs/d.jpg", null), agentToken, 201);
            patch("/api/admin/courier-documents/" + doc.get("id").asText() + "/review",
                    new ReviewCourierDocumentRequest(CourierDocumentStatus.VALID, null), superToken, 200);
        }
        JsonNode apply = post("/api/sellers/apply", new ApplySellerRequest("Boutique Sc " + rand, null), seller.get("accessToken").asText(), 201);
        patch("/api/sellers/admin/" + apply.get("id").asText() + "/status", new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), superToken, 200);
        sellerToken = login(phone("833"), "Passw0rd!");
        shopId = UUID.fromString(post("/api/shops", new CreateShopRequest("Boutique Sc " + rand, "d", "Lomé", "Centre", null, null, null), sellerToken, 201).get("id").asText());
        patch("/api/admin/shops/" + shopId + "/status", new UpdateShopStatusRequest(ShopStatus.APPROVED), superToken, 200);
        categoryId = UUID.fromString(post("/api/categories", new CreateCategoryRequest("Cat Sc " + rand, null, null, null), superToken, 201).get("id").asText());
        addressId = UUID.fromString(post("/api/users/me/addresses", new CreateAddressRequest("Maison", "Lomé", "Bè", "detail", 6.13, 1.22, true), buyerToken, 201).get("id").asText());

        // Prix de base choisis pour que le prix AFFICHE (commission integree 5 %) tombe pile sur le seuil visé
        createProduct("p2499", "2380");   // 2 380 + 119 = 2 499
        createProduct("p2500", "2381");   // 2 381 + 119 = 2 500
        createProduct("p10000", "9524");  // 9 524 + 476 = 10 000
        createProduct("p10001", "9525");  // 9 525 + 476 = 10 001
        createProduct("p15000", "14286"); // 14 286 + 714 = 15 000

        profileA = post("/api/reseller/apply", Map.of("acceptTerms", true), partnerAToken, 201).get("id").asText();
        profileB = post("/api/reseller/apply", Map.of("acceptTerms", true), partnerBToken, 201).get("id").asText();
        patch("/api/admin/resellers/" + profileA + "/approve", null, support.token(), 200);
        patch("/api/admin/resellers/" + profileB + "/approve", null, support.token(), 200);
    }

    @Test
    @Order(2)
    void scenarios3and4_eligibilityAroundThreshold() throws Exception {
        // 3 : 2 499 FCFA non eligible ; 4 : 2 500 FCFA eligible
        post("/api/reseller/links", Map.of("productId", productIds.get("p2499").toString()), partnerAToken, 409);
        Set<String> inCatalog = new HashSet<>();
        for (JsonNode i : get("/api/reseller/catalog?size=100", partnerAToken, 200).get("content")) {
            inCatalog.add(i.get("productId").asText());
            assertFalse(i.has("commissionRate"), "scenario 33 : aucun taux montre au partenaire");
        }
        assertFalse(inCatalog.contains(productIds.get("p2499").toString()));
        assertTrue(inCatalog.contains(productIds.get("p2500").toString()));
        for (String k : List.of("p2500", "p10000", "p10001", "p15000")) {
            codesA.put(k, link(partnerAToken, k));
            codesB.put(k, link(partnerBToken, k));
        }
    }

    @Test
    @Order(3)
    void scenarios5to7_9_12_15_29_tiersRoundingIndependenceAndCredit() throws Exception {
        String tokA10000 = click(codesA.get("p10000"), "va1", buyerToken);
        String tokA10001 = click(codesA.get("p10001"), "va2", buyerToken);
        String tokA15000 = click(codesA.get("p15000"), "va3", buyerToken);
        String tokB2500 = click(codesB.get("p2500"), "vb1", buyerToken);
        // 29 : un partenaire avec plusieurs produits ; 12 : deux produits, deux partenaires
        deliveredOrder = orderMulti(Map.of("p10000", 3, "p10001", 1, "p15000", 1, "p2500", 1),
                List.of(tokA10000, tokA10001, tokA15000, tokB2500), null);

        // Avant livraison : commissions en attente, rien de credite
        assertEquals(0, available(partnerAToken));
        assertEquals("PENDING", commissions(partnerAToken, "p15000").get(0).get("status").asText());

        deliver(deliveredOrder);

        // 5 : 10 000 FCFA -> 5 % = 500, part 125 (x3 unites : une ligne par unite)
        List<JsonNode> c10000 = commissions(partnerAToken, "p10000");
        assertEquals(3, c10000.size());
        for (JsonNode c : c10000) {
            assertEquals(10000, c.get("unitPrice").asLong());
            assertEquals(500, c.get("maplenouCommission").asLong());
            assertEquals(125, c.get("partnerShare").asLong());
            assertEquals("CREDITED", c.get("status").asText());
            assertFalse(c.has("commissionRate"), "scenario 33");
        }
        // 6 : 10 001 FCFA -> 3 % = 300, part 75 (la bascule de palier se fait strictement au-dessus de 10 000)
        JsonNode c10001 = commissions(partnerAToken, "p10001").get(0);
        assertEquals(10001, c10001.get("unitPrice").asLong());
        assertEquals(300, c10001.get("maplenouCommission").asLong());
        assertEquals(75, c10001.get("partnerShare").asLong());
        // 7 : 15 000 FCFA -> 450, part 113 ; 9 : les frais de livraison ne sont pas dans le prix retenu
        JsonNode c15000 = commissions(partnerAToken, "p15000").get(0);
        assertEquals(15000, c15000.get("unitPrice").asLong());
        assertEquals(450, c15000.get("maplenouCommission").asLong());
        assertEquals(113, c15000.get("partnerShare").asLong());
        // 15 + 29 : credit a la livraison, commissions independantes : 3x125 + 75 + 113
        assertEquals(3 * 125 + 75 + 113, available(partnerAToken));
        // 12 : le produit du partenaire B n'a rien donne a A, et B est credite seul (2 500 -> 125, part 31)
        assertTrue(commissions(partnerAToken, "p2500").isEmpty());
        JsonNode cB = commissions(partnerBToken, "p2500").get(0);
        assertEquals(31, cB.get("partnerShare").asLong());
        assertEquals(31, available(partnerBToken));
    }

    @Test
    @Order(4)
    void scenario10_lastValidClickWins() throws Exception {
        String tokA = click(codesA.get("p10000"), "vv1", buyerToken);
        String tokB = click(codesB.get("p10000"), "vv2", buyerToken); // plus recent
        int beforeA = commissions(partnerAToken, "p10000").size();
        int beforeB = commissions(partnerBToken, "p10000").size();
        order("p10000", 1, List.of(tokA, tokB), null);
        assertEquals(beforeA, commissions(partnerAToken, "p10000").size(), "A n'a pas la vente");
        assertEquals(beforeB + 1, commissions(partnerBToken, "p10000").size(), "B (dernier clic) a la vente");
        // Meme sans jeton, l'attribution rattachee au compte acheteur est le dernier clic (B)
        cancelPendingOrders();
    }

    private void cancelPendingOrders() throws Exception {
        for (JsonNode o : get("/api/orders?size=50", buyerToken, 200).get("content")) {
            if ("CREATED".equals(o.get("status").asText())) {
                patch("/api/orders/" + o.get("id").asText() + "/cancel", null, buyerToken, 200);
            }
        }
    }

    @Test
    @Order(5)
    void scenario11_expiredClickGivesNoAttribution() throws Exception {
        String tok = click(codesA.get("p2500"), "ve1", buyerToken);
        UUID clickId = UUID.fromString(tok.substring(0, tok.indexOf('.')));
        jdbc.update("UPDATE reseller_clicks SET expires_at = NOW() - INTERVAL '1 day' WHERE id = ?", clickId);
        jdbc.update("UPDATE reseller_buyer_attributions SET expires_at = NOW() - INTERVAL '1 day' WHERE click_id = ?", clickId);
        int before = commissions(partnerAToken, "p2500").size();
        order("p2500", 1, List.of(tok), null);
        assertEquals(before, commissions(partnerAToken, "p2500").size(), "un clic de plus de 30 jours n'attribue rien");
        cancelPendingOrders();
    }

    @Test
    @Order(6)
    void scenario13_selfPurchaseGivesNoCommission() throws Exception {
        // Le partenaire A clique anonymement sur son propre lien (jeton valide) puis achete connecte
        String tok = click(codesA.get("p2500"), "vself", null);
        assertNotNull(tok);
        String addr = post("/api/users/me/addresses", new CreateAddressRequest("Maison", "Lomé", "Bè", "detail", 6.13, 1.22, true), partnerAToken, 201).get("id").asText();
        post("/api/cart/items", new AddToCartRequest(variantIds.get("p2500"), 1), partnerAToken, 200);
        int before = commissions(partnerAToken, "p2500").size();
        post("/api/orders", new PlaceOrderRequest(UUID.fromString(addr), null, null, null, List.of(tok)), partnerAToken, 201);
        assertEquals(before, commissions(partnerAToken, "p2500").size(), "aucune commission sur un auto-achat");
    }

    @Test
    @Order(7)
    void scenario14_cancelledBeforeDeliveryGivesNoCommission() throws Exception {
        String tok = click(codesA.get("p10001"), "vc1", buyerToken);
        JsonNode o = order("p10001", 1, List.of(tok), null);
        long before = available(partnerAToken);
        patch("/api/orders/" + o.get("id").asText() + "/cancel", null, buyerToken, 200);
        List<JsonNode> list = commissions(partnerAToken, "p10001");
        assertTrue(list.stream().anyMatch(c -> "CANCELLED".equals(c.get("status").asText())));
        assertEquals(before, available(partnerAToken));
    }

    @Test
    @Order(8)
    void scenario8_discountedPriceBelowMinimumGivesNoCommission() throws Exception {
        post("/api/promo-codes", new CreatePromoCodeRequest("SC" + rand, PromoScopeType.PLATFORM, null, new BigDecimal("10.00"), null, null, null), superToken, 201);
        String tok = click(codesA.get("p2500"), "vd1", buyerToken);
        int before = commissions(partnerAToken, "p2500").size();
        order("p2500", 1, List.of(tok), "SC" + rand); // 2 500 - 10 % = 2 250 < 2 500
        assertEquals(before, commissions(partnerAToken, "p2500").size());
        cancelPendingOrders();
    }

    @Test
    @Order(9)
    void scenarios17and26_doubleCreditIsBlocked() throws Exception {
        long before = available(partnerAToken);
        UUID subOrderId = UUID.fromString(deliveredOrder.get("subOrders").get(0).get("id").asText());
        // Le meme evenement de livraison rejoue directement sur le service : aucun second credit
        new TransactionTemplate(txManager).executeWithoutResult(s ->
                attributionService.onSubOrderDelivered(subOrderRepository.findByIdWithDetails(subOrderId).orElseThrow()));
        assertEquals(before, available(partnerAToken));
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM reseller_wallet_transactions WHERE type = 'COMMISSION_CREDIT' AND partner_id = ?::uuid",
                Integer.class, profileA);
        assertEquals(5, rows, "3 + 1 + 1 ecritures de credit, pas une de plus");
    }

    @Test
    @Order(10)
    void scenario18_returnAfterCreditKeepsCommission() throws Exception {
        long before = available(partnerAToken);
        UUID subOrderId = UUID.fromString(deliveredOrder.get("subOrders").get(0).get("id").asText());
        JsonNode rr = post("/api/return-requests",
                new CreateReturnRequest(subOrderId, "Le client a change d'avis sur ce produit", ProductCondition.NEW), buyerToken, 201);
        patch("/api/return-requests/" + rr.get("id").asText() + "/decision",
                new DecideReturnRequest(ReturnDecision.APPROVED, "Retour accepte"), sellerToken, 200);
        assertEquals(before, available(partnerAToken), "le retour ne reprend pas la commission creditee");
        assertTrue(commissions(partnerAToken, "p15000").stream().allMatch(c -> "CREDITED".equals(c.get("status").asText())));
    }

    @Test
    @Order(11)
    void scenarios23and24_suspensionKeepsPendingAndBlocksNewAttributions() throws Exception {
        String tok = click(codesB.get("p10001"), "vs1", buyerToken);
        JsonNode o = order("p10001", 1, List.of(tok), null); // commission en attente avant la suspension
        patch("/api/admin/resellers/" + profileB + "/suspend", Map.of("reason", "Controle"), support.token(), 200);

        // 23 : liens inactifs pour de nouvelles attributions (clic invalide, jeton ancien ignore a la commande)
        assertNull(click(codesB.get("p10001"), "vs2", buyerToken));
        String oldTok = tok;
        int before = commissions(partnerBToken, "p10001").size();
        order("p10001", 1, List.of(oldTok), null);
        assertEquals(before, commissions(partnerBToken, "p10001").size(), "pas de nouvelle commission pour un partenaire suspendu");

        // 24 : la commission deja en attente est creditee si la livraison est validee
        long bBefore = available(partnerBToken);
        deliver(o);
        assertEquals(bBefore + 75, available(partnerBToken));
        patch("/api/admin/resellers/" + profileB + "/reactivate", Map.of("reason", "ok"), support.token(), 200);
        cancelPendingOrders();
    }

    @Test
    @Order(12)
    void scenario28_unavailableProductBlocksNewAttributions() throws Exception {
        patch("/api/shops/mine/products/" + productIds.get("p10001"),
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.DRAFT, null, null, null, null), sellerToken, 200);
        assertNull(click(codesA.get("p10001"), "vu1", buyerToken), "produit indisponible : plus de nouvelle attribution");
        // Les commissions deja enregistrees sont conservees
        assertFalse(commissions(partnerAToken, "p10001").isEmpty());
        patch("/api/shops/mine/products/" + productIds.get("p10001"),
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null, null, null, null), sellerToken, 200);
    }

    @Test
    @Order(13)
    void scenario32_statsWithPeriodFilters() throws Exception {
        for (int days : new int[]{7, 30, 90}) {
            JsonNode s = get("/api/reseller/stats?period=" + days, partnerAToken, 200);
            assertTrue(s.get("clicks").asLong() >= 1);
            assertTrue(s.get("attributedSales").asLong() >= 1);
            assertTrue(s.get("unitsSold").asLong() >= 1);
            assertTrue(s.get("conversionRate").decimalValue().signum() >= 0);
        }
        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        String yesterday = java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1).toString();
        assertTrue(get("/api/reseller/stats?from=" + yesterday + "&to=" + today, partnerAToken, 200).get("clicks").asLong() >= 1);
        JsonNode old = get("/api/reseller/stats?from=2020-01-01&to=2020-01-31", partnerAToken, 200);
        assertEquals(0, old.get("clicks").asLong());
        get("/api/reseller/stats", partnerAToken, 400);
        get("/api/reseller/stats?period=15", partnerAToken, 400);
        // Chaque partenaire ne voit que ses propres chiffres
        get("/api/reseller/stats?period=30", buyerToken, 403);
    }

    @Test
    @Order(14)
    void scenario33_noCommissionRateExposedToPartners() throws Exception {
        JsonNode s = get("/api/reseller/settings", partnerAToken, 200);
        assertFalse(s.has("rateLow"));
        assertFalse(s.has("rateHigh"));
        assertFalse(s.has("tierThreshold"));
        assertTrue(s.has("shareRate"));
        // Cote admin les taux restent visibles
        assertTrue(get("/api/admin/resellers/settings", superToken, 200).has("rateLow"));
    }

    @Test
    @Order(15)
    void scenario34_programStopKeepsBalancesAndHistory() throws Exception {
        long balanceA = available(partnerAToken);
        int commissionsA = get("/api/reseller/commissions?size=100", partnerAToken, 200).get("content").size();
        put("/api/admin/resellers/settings", settings(false, "Arret du programme (test)"), superToken, 200);
        try {
            // Plus de nouvelles demandes, de liens ni d'attributions
            String newbie = post("/api/auth/register", new RegisterRequest("Sc Newbie", phone("835"), "Passw0rd!", null), null, 201).get("accessToken").asText();
            post("/api/reseller/apply", Map.of("acceptTerms", true), newbie, 409);
            post("/api/reseller/links", Map.of("productId", productIds.get("p10000").toString()), partnerAToken, 409);
            assertNull(click(codesA.get("p10000"), "vz1", buyerToken));
            // Soldes dus et historique conserves et consultables
            assertEquals(balanceA, available(partnerAToken));
            assertEquals(commissionsA, get("/api/reseller/commissions?size=100", partnerAToken, 200).get("content").size());
            get("/api/reseller/wallet/transactions", partnerAToken, 200);
            get("/api/reseller/withdrawals", partnerAToken, 200);
            assertFalse(get("/api/reseller/settings", partnerAToken, 200).get("programActive").asBoolean());
        } finally {
            put("/api/admin/resellers/settings", settings(true, "Reouverture apres test"), superToken, 200);
        }
        assertTrue(get("/api/reseller/settings", partnerAToken, 200).get("programActive").asBoolean());
    }

    @Test
    @Order(16)
    void catalogShowsPerProductPerformance() throws Exception {
        boolean found = false;
        for (JsonNode i : get("/api/reseller/catalog?size=100", partnerAToken, 200).get("content")) {
            if (i.get("productId").asText().equals(productIds.get("p10000").toString())) {
                found = true;
                assertTrue(i.get("clicks").asLong() >= 1);
                assertTrue(i.get("unitsSold").asLong() >= 3, "3 unites vendues via le lien de A");
                assertEquals(codesA.get("p10000"), i.get("linkCode").asText());
            }
        }
        assertTrue(found);
    }

    @Test
    @Order(17)
    void partnerReceivesOnlyTheRequiredNotifications() throws Exception {
        // Vente attribuee + commission creditee : seules notifications requises (pas de statut de compte, pas de clic)
        Set<String> titles = new HashSet<>();
        for (int attempt = 0; attempt < 20; attempt++) {
            titles.clear();
            for (JsonNode n : get("/api/notifications?size=100", partnerAToken, 200).get("content")) {
                titles.add(n.get("title").asText());
            }
            if (titles.contains("Commission créditée") && titles.contains("Vente attribuée à votre lien")) break;
            Thread.sleep(300);
        }
        assertTrue(titles.contains("Vente attribuée à votre lien"), "notification de vente attribuee : " + titles);
        assertTrue(titles.contains("Commission créditée"), "notification de credit : " + titles);
        assertFalse(titles.stream().anyMatch(t -> t.toLowerCase().contains("suspendu") || t.toLowerCase().contains("approuvée")),
                "pas de notification de changement de statut du compte : " + titles);
    }
}
