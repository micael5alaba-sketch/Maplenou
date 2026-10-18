package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.catalog.ProductStatus;
import com.maplenou.backend.catalog.ShopStatus;
import com.maplenou.backend.courier.CourierDocumentStatus;
import com.maplenou.backend.courier.CourierDocumentType;
import com.maplenou.backend.courier.dto.ReviewCourierDocumentRequest;
import com.maplenou.backend.courier.dto.SubmitCourierDocumentRequest;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.delivery.ProofType;
import com.maplenou.backend.delivery.dto.AssignAgentRequest;
import com.maplenou.backend.delivery.dto.UpdateDeliveryRequest;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.order.dto.PaymentWebhookRequest;
import com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest;
import com.maplenou.backend.payout.dto.GeneratePayoutsRequest;
import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie bout-en-bout le barème "Exigences des nouveaux frais vendeurs" : 3 premières ventes
 * éligibles gratuites, frais unique à partir de la 4ᵉ (idempotent dans le mois), déduit du
 * reversement avant versement. Flux 100% réel (HTTP, vraie base) comme le reste de la suite —
 * aucun mock.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SellerFeeIntegrationTest extends AbstractIntegrationTest {

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    private final long rand = System.nanoTime() % 100_000_000L;

    private String adminToken;
    private String buyerToken;
    private String sellerToken;
    private String agentToken;

    private UUID agentId, shopId, variantId, addressId, sellerProfileId;

    @Test
    @Order(1)
    void setupSellerShopAndProductWithHighPriceCategory() throws Exception {
        adminToken = login("+22890000000", "Micael2005@");

        String buyerPhone = "+22873" + String.format("%07d", rand % 10_000_000L);
        String sellerPhone = "+22874" + String.format("%07d", rand % 10_000_000L);
        String agentPhone = "+22875" + String.format("%07d", rand % 10_000_000L);

        JsonNode buyerAuth = post("/api/auth/register", new RegisterRequest("Fee Buyer", buyerPhone, "Passw0rd!", null), null, 201);
        buyerToken = buyerAuth.get("accessToken").asText();

        JsonNode sellerAuth = post("/api/auth/register", new RegisterRequest("Fee Seller", sellerPhone, "Passw0rd!", null), null, 201);

        JsonNode agentAuth = post("/api/auth/register", new RegisterRequest("Fee Agent", agentPhone, "Passw0rd!", null), null, 201);
        agentId = UUID.fromString(agentAuth.get("user").get("id").asText());
        patch("/api/admin/users/" + agentId + "/role", new ChangeRoleRequest(Role.DELIVERY_AGENT), adminToken, 200);
        agentToken = login(agentPhone, "Passw0rd!");

        for (CourierDocumentType type : CourierDocumentType.values()) {
            JsonNode submitted = post("/api/delivery-agents/me/documents",
                    new SubmitCourierDocumentRequest(type, "https://res.cloudinary.com/demo/image/upload/v1/docs/d.jpg", null),
                    agentToken, 201);
            UUID documentId = UUID.fromString(submitted.get("id").asText());
            patch("/api/admin/courier-documents/" + documentId + "/review",
                    new ReviewCourierDocumentRequest(CourierDocumentStatus.VALID, null), adminToken, 200);
        }

        // HIGH_PRICE choisi explicitement à l'inscription -> frais attendu = 2000.00 FCFA/mois.
        JsonNode apply = post("/api/sellers/apply",
                new ApplySellerRequest("Boutique Frais " + rand, SellerFeeCategory.HIGH_PRICE),
                sellerAuth.get("accessToken").asText(), 201);
        sellerProfileId = UUID.fromString(apply.get("id").asText());
        patch("/api/sellers/admin/" + sellerProfileId + "/status",
                new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), adminToken, 200);
        sellerToken = login(sellerPhone, "Passw0rd!");

        JsonNode shop = post("/api/shops", new CreateShopRequest("Boutique Frais " + rand, "d", "Lomé", "Centre", null, null, null), sellerToken, 201);
        shopId = UUID.fromString(shop.get("id").asText());
        patch("/api/admin/shops/" + shopId + "/status", new UpdateShopStatusRequest(ShopStatus.APPROVED), adminToken, 200);

        JsonNode category = post("/api/categories", new CreateCategoryRequest("Cat Frais " + rand, null, null, null), adminToken, 201);
        UUID categoryId = UUID.fromString(category.get("id").asText());

        JsonNode product = post("/api/shops/mine/products",
                new CreateProductRequest("Produit Frais " + rand, "desc", new BigDecimal("15000.00"), null, categoryId,
                        List.of(new CreateVariantRequest("Standard", null, 20, "SKU-FEE-" + rand)), null,
                        null, null, null),
                sellerToken, 201);
        variantId = UUID.fromString(product.get("variants").get(0).get("id").asText());
        UUID productId = UUID.fromString(product.get("id").asText());

        patch("/api/shops/mine/products/" + productId,
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null, null, null, null), sellerToken, 200);

        JsonNode address = post("/api/users/me/addresses",
                new CreateAddressRequest("Maison", "Lomé", "Bè", "detail", 6.13, 1.22, true), buyerToken, 201);
        addressId = UUID.fromString(address.get("id").asText());
    }

    /** Un cycle complet commande -> paiement -> préparation -> livraison -> DELIVERED. */
    private void deliverOneOrder() throws Exception {
        post("/api/cart/items", new AddToCartRequest(variantId, 1), buyerToken, 200);
        JsonNode order = post("/api/orders", new PlaceOrderRequest(addressId, null, null, null), buyerToken, 201);
        UUID orderId = UUID.fromString(order.get("id").asText());
        UUID subOrderId = UUID.fromString(order.get("subOrders").get(0).get("id").asText());

        postWithHeader("/api/webhooks/payment/confirmed",
                new PaymentWebhookRequest(orderId.toString(), "PAY-FEE-" + rand + "-" + System.nanoTime()),
                "X-Webhook-Secret", webhookSecret, 200);

        patch("/api/sub-orders/" + subOrderId + "/status",
                new UpdateSubOrderStatusRequest(SubOrderStatus.PREPARING), sellerToken, 200);
        patch("/api/sub-orders/" + subOrderId + "/status",
                new UpdateSubOrderStatusRequest(SubOrderStatus.READY_FOR_PICKUP), sellerToken, 200);

        JsonNode delivery = post("/api/deliveries/sub-orders/" + subOrderId, null, adminToken, 201);
        UUID deliveryId = UUID.fromString(delivery.get("id").asText());
        patch("/api/deliveries/" + deliveryId + "/assign", new AssignAgentRequest(agentId), adminToken, 200);

        patch("/api/deliveries/" + deliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.IN_TRANSIT, null, null, null), agentToken, 200);
        patch("/api/deliveries/" + deliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.DELIVERED, ProofType.PHOTO,
                        "https://res.cloudinary.com/demo/image/upload/v1/proofs/p.jpg", null),
                agentToken, 200);
    }

    @Test
    @Order(2)
    void firstThreeEligibleSalesAreFree() throws Exception {
        deliverOneOrder();
        deliverOneOrder();
        deliverOneOrder();

        JsonNode summary = get("/api/seller/fees/summary", sellerToken, 200);
        assertEquals("HIGH_PRICE", summary.get("category").asText());
        assertEquals(0, new BigDecimal("0").compareTo(summary.get("unpaidFeesTotal").decimalValue()));

        JsonNode history = get("/api/seller/fees/history", sellerToken, 200);
        assertEquals(0, history.get("content").size());
    }

    @Test
    @Order(3)
    void fourthEligibleSaleCreatesExactlyOneFee() throws Exception {
        deliverOneOrder(); // 4ᵉ vente éligible -> premier frais du mois

        JsonNode summary = get("/api/seller/fees/summary", sellerToken, 200);
        assertEquals(0, new BigDecimal("2000.00").compareTo(summary.get("unpaidFeesTotal").decimalValue()));

        JsonNode history = get("/api/seller/fees/history", sellerToken, 200);
        assertEquals(1, history.get("content").size());
        assertEquals("PENDING", history.get("content").get(0).get("status").asText());
        assertEquals(0, new BigDecimal("2000.00").compareTo(history.get("content").get(0).get("amount").decimalValue()));
    }

    @Test
    @Order(4)
    void fifthEligibleSaleSameMonthDoesNotDuplicateTheFee() throws Exception {
        deliverOneOrder(); // 5ᵉ vente, même mois -> ne doit pas créer un deuxième frais

        JsonNode summary = get("/api/seller/fees/summary", sellerToken, 200);
        // Toujours 2000, jamais 4000 : un seul frais par (boutique, mois).
        assertEquals(0, new BigDecimal("2000.00").compareTo(summary.get("unpaidFeesTotal").decimalValue()));

        JsonNode history = get("/api/seller/fees/history", sellerToken, 200);
        assertEquals(1, history.get("content").size());
    }

    @Test
    @Order(5)
    void payoutGenerationDeductsUnpaidFeeBeforePayingSeller() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        post("/api/admin/payouts/generate", new GeneratePayoutsRequest(from, to), adminToken, 201);

        JsonNode payouts = get("/api/seller/payouts", sellerToken, 200);
        assertTrue(payouts.get("content").size() >= 1);
        JsonNode payout = payouts.get("content").get(0);

        assertEquals(0, new BigDecimal("2000.00").compareTo(payout.get("feesDeducted").decimalValue()));

        // Le frais est maintenant payé -> plus rien à déduire au prochain reversement.
        JsonNode summary = get("/api/seller/fees/summary", sellerToken, 200);
        assertEquals(0, new BigDecimal("0").compareTo(summary.get("unpaidFeesTotal").decimalValue()));

        JsonNode history = get("/api/seller/fees/history", sellerToken, 200);
        assertEquals("PAID", history.get("content").get(0).get("status").asText());
    }

    @Test
    @Order(6)
    void categoryChangeRequestIsRejectedWhenAlreadyPendingOrSameCategory() throws Exception {
        post("/api/seller/fees/category-change",
                new com.maplenou.backend.sellerfee.dto.RequestCategoryChangeRequest(SellerFeeCategory.HIGH_PRICE),
                sellerToken, 400); // déjà la catégorie active

        JsonNode change = post("/api/seller/fees/category-change",
                new com.maplenou.backend.sellerfee.dto.RequestCategoryChangeRequest(SellerFeeCategory.STANDARD),
                sellerToken, 201);
        assertEquals("PENDING", change.get("status").asText());

        // Une deuxième demande alors que la première est encore en attente -> refusée
        post("/api/seller/fees/category-change",
                new com.maplenou.backend.sellerfee.dto.RequestCategoryChangeRequest(SellerFeeCategory.STANDARD),
                sellerToken, 409);

        UUID changeId = UUID.fromString(change.get("id").asText());
        JsonNode decided = patch("/api/admin/seller-fees/category-changes/" + changeId + "/decide",
                new com.maplenou.backend.sellerfee.dto.DecideCategoryChangeRequest(true, "ok"), adminToken, 200);
        assertEquals("APPROVED", decided.get("status").asText());
        assertNotNull(decided.get("effectiveFrom"));
    }
}
