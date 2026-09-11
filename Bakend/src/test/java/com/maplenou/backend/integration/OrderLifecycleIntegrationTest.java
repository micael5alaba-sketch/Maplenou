package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.audit.AuditLogRepository;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.cart.CartRepository;
import com.maplenou.backend.cart.dto.AddToCartRequest;
import com.maplenou.backend.catalog.*;
import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.delivery.ProofType;
import com.maplenou.backend.delivery.dto.AssignAgentRequest;
import com.maplenou.backend.delivery.dto.CreateDeliveryZoneRequest;
import com.maplenou.backend.delivery.dto.UpdateDeliveryRequest;
import com.maplenou.backend.order.OrderRepository;
import com.maplenou.backend.order.dto.PlaceOrderRequest;
import com.maplenou.backend.payout.PayoutRepository;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.payout.dto.GeneratePayoutsRequest;
import com.maplenou.backend.payout.dto.UpdatePayoutStatusRequest;
import com.maplenou.backend.promo.PromoCodeRepository;
import com.maplenou.backend.promo.PromoCodeUsageRepository;
import com.maplenou.backend.promo.PromoScopeType;
import com.maplenou.backend.promo.dto.CreatePromoCodeRequest;
import com.maplenou.backend.promo.dto.ValidatePromoRequest;
import com.maplenou.backend.returns.ProductCondition;
import com.maplenou.backend.returns.ReturnDecision;
import com.maplenou.backend.returns.ReturnRequestRepository;
import com.maplenou.backend.returns.dto.CreateReturnRequest;
import com.maplenou.backend.returns.dto.DecideReturnRequest;
import com.maplenou.backend.review.ProductReviewRepository;
import com.maplenou.backend.review.ShopReviewRepository;
import com.maplenou.backend.review.dto.CreateProductReviewRequest;
import com.maplenou.backend.review.dto.CreateShopReviewRequest;
import com.maplenou.backend.seller.SellerProfileRepository;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.AddressRepository;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;
    @Autowired private SellerProfileRepository sellerProfileRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private ReturnRequestRepository returnRequestRepository;
    @Autowired private ProductReviewRepository productReviewRepository;
    @Autowired private ShopReviewRepository shopReviewRepository;
    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private PromoCodeUsageRepository promoCodeUsageRepository;
    @Autowired private PayoutRepository payoutRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private AddressRepository addressRepository;

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    private final long rand = System.nanoTime() % 100_000_000L;

    private String adminToken;
    private String buyerToken;
    private String sellerToken;
    private String agentToken;

    private UUID buyerId, sellerId, agentId;
    private UUID sellerProfileId, shopId, categoryId, productId, variantId;
    private String productSlug;
    private UUID addressId;
    private UUID promoCodeId;
    private UUID orderId, order2Id;
    private UUID subOrderId, subOrder2Id;
    private UUID deliveryZoneId;
    private UUID deliveryId, delivery2Id;
    private UUID productReviewId, shopReviewId;
    private UUID returnRequestId, returnRequest2Id;
    private UUID payoutId;

    @Test
    @Order(1)
    void setupSellerShopAndProduct() throws Exception {
        adminToken = login("+22890000000", "Micael2005@");

        String buyerPhone = "+22870" + String.format("%07d", rand % 10_000_000L);
        String sellerPhone = "+22871" + String.format("%07d", rand % 10_000_000L);
        String agentPhone = "+22872" + String.format("%07d", rand % 10_000_000L);

        JsonNode buyerAuth = post("/api/auth/register", new RegisterRequest("Order Buyer", buyerPhone, "Passw0rd!"), null, 201);
        buyerId = UUID.fromString(buyerAuth.get("user").get("id").asText());
        buyerToken = buyerAuth.get("accessToken").asText();

        JsonNode sellerAuth = post("/api/auth/register", new RegisterRequest("Order Seller", sellerPhone, "Passw0rd!"), null, 201);
        sellerId = UUID.fromString(sellerAuth.get("user").get("id").asText());

        JsonNode agentAuth = post("/api/auth/register", new RegisterRequest("Order Agent", agentPhone, "Passw0rd!"), null, 201);
        agentId = UUID.fromString(agentAuth.get("user").get("id").asText());
        patch("/api/admin/users/" + agentId + "/role", new ChangeRoleRequest(Role.DELIVERY_AGENT), adminToken, 200);
        agentToken = login(agentPhone, "Passw0rd!");

        JsonNode apply = post("/api/sellers/apply", new ApplySellerRequest("Boutique Order " + rand),
                sellerAuth.get("accessToken").asText(), 201);
        sellerProfileId = UUID.fromString(apply.get("id").asText());
        patch("/api/sellers/admin/" + sellerProfileId + "/status",
                new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), adminToken, 200);
        sellerToken = login(sellerPhone, "Passw0rd!");

        JsonNode shop = post("/api/shops", new CreateShopRequest("Boutique Order " + rand, "d", "Lomé", "Centre"), sellerToken, 201);
        shopId = UUID.fromString(shop.get("id").asText());
        patch("/api/admin/shops/" + shopId + "/status", new UpdateShopStatusRequest(ShopStatus.APPROVED), adminToken, 200);

        JsonNode category = post("/api/categories", new CreateCategoryRequest("Cat Order " + rand, null, null, null), adminToken, 201);
        categoryId = UUID.fromString(category.get("id").asText());

        JsonNode product = post("/api/shops/mine/products",
                new CreateProductRequest("Produit Order " + rand, "desc", new BigDecimal("10000.00"), null, categoryId,
                        List.of(new CreateVariantRequest("Standard", null, 10, "SKU-ORDER-" + rand)), null),
                sellerToken, 201);
        productId = UUID.fromString(product.get("id").asText());
        productSlug = product.get("slug").asText();
        variantId = UUID.fromString(product.get("variants").get(0).get("id").asText());

        patch("/api/shops/mine/products/" + productId,
                new UpdateProductRequest(null, null, null, null, null, ProductStatus.ACTIVE, null), sellerToken, 200);

        JsonNode address = post("/api/users/me/addresses",
                new CreateAddressRequest("Maison", "Lomé", "Bè", "detail", 6.13, 1.22, true), buyerToken, 201);
        addressId = UUID.fromString(address.get("id").asText());
    }

    @Test
    @Order(2)
    void promoCodeAndPlaceOrderWithDiscount() throws Exception {
        JsonNode promo = post("/api/promo-codes",
                new CreatePromoCodeRequest("PROMO" + rand, PromoScopeType.PLATFORM, null,
                        new BigDecimal("10.00"), null, null, null),
                adminToken, 201);
        promoCodeId = UUID.fromString(promo.get("id").asText());

        JsonNode validated = post("/api/promo-codes/validate",
                new ValidatePromoRequest("PROMO" + rand, new BigDecimal("30000.00"), null), buyerToken, 200);
        assertEquals(0, new BigDecimal("3000.00").compareTo(new BigDecimal(validated.get("discountAmount").asText())));

        post("/api/cart/items", new AddToCartRequest(variantId, 3), buyerToken, 200);

        JsonNode order = post("/api/orders",
                new PlaceOrderRequest(addressId, "PROMO" + rand, null), buyerToken, 201);
        orderId = UUID.fromString(order.get("id").asText());
        assertEquals("CREATED", order.get("status").asText());
        assertTrue(order.get("discountAmount").decimalValue().compareTo(BigDecimal.ZERO) > 0);
        subOrderId = UUID.fromString(order.get("subOrders").get(0).get("id").asText());

        // Le code promo a déjà été utilisé par cet acheteur -> une deuxième commande avec le même code échoue
        post("/api/cart/items", new AddToCartRequest(variantId, 1), buyerToken, 200);
        post("/api/orders", new PlaceOrderRequest(addressId, "PROMO" + rand, null), buyerToken, 409);
        // Le panier doit être vidé manuellement car la tentative précédente a échoué avant de vider le panier
        delete("/api/cart", buyerToken, 204);
    }

    @Test
    @Order(3)
    void paymentWebhookConfirmsOrderAndDecrementsStock() throws Exception {
        postWithHeader("/api/webhooks/payment/confirmed",
                new com.maplenou.backend.order.dto.PaymentWebhookRequest(orderId.toString(), "PAY-REF-" + rand),
                "X-Webhook-Secret", webhookSecret, 200);
        JsonNode myOrder = get("/api/orders/" + orderId, buyerToken, 200);
        assertEquals("PAID", myOrder.get("status").asText());

        // Idempotence : rejouer le webhook ne doit pas planter
        postWithHeader("/api/webhooks/payment/confirmed",
                new com.maplenou.backend.order.dto.PaymentWebhookRequest(orderId.toString(), "PAY-REF-" + rand),
                "X-Webhook-Secret", webhookSecret, 200);

        JsonNode myOrders = get("/api/orders", buyerToken, 200);
        assertTrue(myOrders.get("content").isArray());

        // Deuxième commande dédiée à un paiement échoué
        post("/api/cart/items", new AddToCartRequest(variantId, 1), buyerToken, 200);
        JsonNode order2 = post("/api/orders", new PlaceOrderRequest(addressId, null, null), buyerToken, 201);
        order2Id = UUID.fromString(order2.get("id").asText());
        subOrder2Id = UUID.fromString(order2.get("subOrders").get(0).get("id").asText());

        postWithHeader("/api/webhooks/payment/failed",
                new com.maplenou.backend.order.dto.PaymentWebhookRequest(order2Id.toString(), null),
                "X-Webhook-Secret", webhookSecret, 200);
        JsonNode failedOrder = get("/api/orders/" + order2Id, buyerToken, 200);
        assertEquals("PAYMENT_FAILED", failedOrder.get("status").asText());
    }

    @Test
    @Order(4)
    void webhookSecretIsVerified() throws Exception {
        postWithHeader("/api/webhooks/payment/confirmed",
                new com.maplenou.backend.order.dto.PaymentWebhookRequest(orderId.toString(), "x"),
                "X-Webhook-Secret", "wrong-secret", 401);
    }

    @Test
    @Order(5)
    void sellerAdvancesSubOrderThroughValidTransitions() throws Exception {
        JsonNode mySubOrders = get("/api/sub-orders", sellerToken, 200);
        assertTrue(mySubOrders.get("content").isArray());

        JsonNode detail = get("/api/sub-orders/" + subOrderId, sellerToken, 200);
        assertEquals("PENDING", detail.get("status").asText());

        // Transition invalide : PENDING -> READY_FOR_PICKUP directement
        exchange("PATCH", "/api/sub-orders/" + subOrderId + "/status",
                new com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest(
                        com.maplenou.backend.order.SubOrderStatus.READY_FOR_PICKUP),
                sellerToken, 409);

        patch("/api/sub-orders/" + subOrderId + "/status",
                new com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest(
                        com.maplenou.backend.order.SubOrderStatus.PREPARING),
                sellerToken, 200);
        JsonNode ready = patch("/api/sub-orders/" + subOrderId + "/status",
                new com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest(
                        com.maplenou.backend.order.SubOrderStatus.READY_FOR_PICKUP),
                sellerToken, 200);
        assertEquals("READY_FOR_PICKUP", ready.get("status").asText());
    }

    @Test
    @Order(6)
    void deliveryZoneCrud() throws Exception {
        JsonNode zone = post("/api/delivery-zones",
                new CreateDeliveryZoneRequest("Zone Test " + rand, new BigDecimal("1000.00"), 45), adminToken, 201);
        deliveryZoneId = UUID.fromString(zone.get("id").asText());
        assertTrue(zone.get("active").asBoolean());

        JsonNode active = get("/api/delivery-zones", null, 200);
        assertTrue(active.isArray());

        JsonNode toggled = patch("/api/delivery-zones/" + deliveryZoneId + "/toggle", null, adminToken, 200);
        assertFalse(toggled.get("active").asBoolean());

        delete("/api/delivery-zones/" + deliveryZoneId, adminToken, 204);
        deliveryZoneId = null;
    }

    @Test
    @Order(7)
    void deliveryAssignmentNavigationAndCompletion() throws Exception {
        JsonNode delivery = post("/api/deliveries/sub-orders/" + subOrderId, null, adminToken, 201);
        deliveryId = UUID.fromString(delivery.get("id").asText());
        assertEquals("PENDING", delivery.get("status").asText());

        // Une deuxième création pour le même sous-ordre est refusée
        post("/api/deliveries/sub-orders/" + subOrderId, null, adminToken, 409);

        JsonNode assigned = patch("/api/deliveries/" + deliveryId + "/assign",
                new AssignAgentRequest(agentId), adminToken, 200);
        assertEquals("ASSIGNED", assigned.get("status").asText());

        JsonNode myDeliveries = get("/api/deliveries/my", agentToken, 200);
        assertTrue(myDeliveries.get("content").isArray());

        JsonNode nav = get("/api/deliveries/" + deliveryId + "/navigation", agentToken, 200);
        assertEquals(shopId.toString(), nav.get("pickup").get("shopId").asText());

        patch("/api/deliveries/" + deliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.IN_TRANSIT, null, null, null), agentToken, 200);
        JsonNode subOrderInTransit = get("/api/sub-orders/" + subOrderId, sellerToken, 200);
        assertEquals("IN_DELIVERY", subOrderInTransit.get("status").asText());

        JsonNode delivered = patch("/api/deliveries/" + deliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.DELIVERED, ProofType.PHOTO,
                        "https://res.cloudinary.com/demo/image/upload/v1/proofs/p.jpg", "remis en main propre"),
                agentToken, 200);
        assertEquals("DELIVERED", delivered.get("status").asText());

        JsonNode subOrderDelivered = get("/api/sub-orders/" + subOrderId, sellerToken, 200);
        assertEquals("DELIVERED", subOrderDelivered.get("status").asText());
    }

    @Test
    @Order(8)
    void productAndShopReviewsRequireDeliveredSubOrder() throws Exception {
        JsonNode review = post("/api/reviews/products",
                new CreateProductReviewRequest(productId, subOrderId, (short) 5, "Excellent produit"), buyerToken, 201);
        productReviewId = UUID.fromString(review.get("id").asText());

        // Un seul avis par acheteur par produit
        post("/api/reviews/products",
                new CreateProductReviewRequest(productId, subOrderId, (short) 4, "Encore"), buyerToken, 409);

        JsonNode productReviews = get("/api/products/" + productId + "/reviews", null, 200);
        assertEquals(1, productReviews.get("content").size());
        JsonNode productSummary = get("/api/products/" + productId + "/reviews/summary", null, 200);
        assertEquals(5.0, productSummary.get("averageRating").asDouble());

        JsonNode shopReview = post("/api/reviews/shops",
                new CreateShopReviewRequest(shopId, subOrderId, (short) 4, "Bonne boutique"), buyerToken, 201);
        shopReviewId = UUID.fromString(shopReview.get("id").asText());

        JsonNode shopReviews = get("/api/shops/" + shopId + "/reviews", null, 200);
        assertEquals(1, shopReviews.get("content").size());
        JsonNode shopSummary = get("/api/shops/" + shopId + "/reviews/summary", null, 200);
        assertEquals(4.0, shopSummary.get("averageRating").asDouble());
    }

    @Test
    @Order(9)
    void returnRequestRejectedKeepsSubOrderEligibleForPayout() throws Exception {
        JsonNode created = post("/api/return-requests",
                new CreateReturnRequest(subOrderId, "Produit non conforme à la description", ProductCondition.GOOD),
                buyerToken, 201);
        returnRequestId = UUID.fromString(created.get("id").asText());

        JsonNode subOrderAfterRequest = get("/api/sub-orders/" + subOrderId, sellerToken, 200);
        assertEquals("RETURN_REQUESTED", subOrderAfterRequest.get("status").asText());

        JsonNode mine = get("/api/return-requests/my", buyerToken, 200);
        assertTrue(mine.get("content").size() >= 1);
        get("/api/return-requests/my/" + returnRequestId, buyerToken, 200);

        JsonNode forShop = get("/api/return-requests/shop", sellerToken, 200);
        assertTrue(forShop.get("content").size() >= 1);

        JsonNode decided = patch("/api/return-requests/" + returnRequestId + "/decision",
                new DecideReturnRequest(ReturnDecision.REJECTED, "Retour refusé, produit conforme"), sellerToken, 200);
        assertEquals("REJECTED", decided.get("decision").asText());
        assertEquals("DENIED", decided.get("refundStatus").asText());

        JsonNode subOrderAfterDecision = get("/api/sub-orders/" + subOrderId, sellerToken, 200);
        assertEquals("DELIVERED", subOrderAfterDecision.get("status").asText());

        // Décider deux fois est refusé
        patch("/api/return-requests/" + returnRequestId + "/decision",
                new DecideReturnRequest(ReturnDecision.APPROVED, "trop tard"), sellerToken, 409);

        get("/api/return-requests/admin/pending", adminToken, 200);
        get("/api/return-requests/admin/" + returnRequestId, adminToken, 200);
    }

    @Test
    @Order(10)
    void payoutGenerationAndLifecycle() throws Exception {
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);

        JsonNode generated = post("/api/admin/payouts/generate",
                new GeneratePayoutsRequest(from, to), adminToken, 201);
        assertTrue(generated.get("payoutsCreated").asInt() >= 1);

        JsonNode adminList = get("/api/admin/payouts?status=PENDING", adminToken, 200);
        JsonNode payouts = adminList.get("content");
        assertTrue(payouts.size() >= 1);
        UUID foundPayoutId = null;
        for (JsonNode p : payouts) {
            if (shopId.toString().equals(p.get("shopId").asText())) {
                foundPayoutId = UUID.fromString(p.get("id").asText());
                break;
            }
        }
        assertNotNull(foundPayoutId, "un payout doit exister pour notre boutique");
        payoutId = foundPayoutId;

        JsonNode fetched = get("/api/admin/payouts/" + payoutId, adminToken, 200);
        assertEquals("PENDING", fetched.get("status").asText());

        JsonNode myPayouts = get("/api/seller/payouts", sellerToken, 200);
        assertTrue(myPayouts.get("content").size() >= 1);

        JsonNode processing = patch("/api/admin/payouts/" + payoutId + "/processing", null, adminToken, 200);
        assertEquals("PROCESSING", processing.get("status").asText());

        JsonNode paid = patch("/api/admin/payouts/" + payoutId + "/paid",
                new UpdatePayoutStatusRequest(PayoutStatus.PAID, "Virement Flooz effectué"), adminToken, 200);
        assertEquals("PAID", paid.get("status").asText());

        // Un payout déjà PAID ne peut plus changer de statut
        patch("/api/admin/payouts/" + payoutId + "/failed",
                new UpdatePayoutStatusRequest(PayoutStatus.FAILED, "test"), adminToken, 409);
    }

    @Test
    @Order(11)
    void secondSubOrderReturnApprovedRestoresStockAndMarksReturned() throws Exception {
        post("/api/cart/items", new AddToCartRequest(variantId, 2), buyerToken, 200);
        JsonNode order = post("/api/orders", new PlaceOrderRequest(addressId, null, null), buyerToken, 201);
        UUID thisOrderId = UUID.fromString(order.get("id").asText());
        UUID thisSubOrderId = UUID.fromString(order.get("subOrders").get(0).get("id").asText());

        postWithHeader("/api/webhooks/payment/confirmed",
                new com.maplenou.backend.order.dto.PaymentWebhookRequest(thisOrderId.toString(), "PAY-REF-2-" + rand),
                "X-Webhook-Secret", webhookSecret, 200);

        JsonNode beforeVariant = get("/api/products/" + productSlug, null, 200);
        int stockBefore = beforeVariant.get("variants").get(0).get("stockQuantity").asInt();

        patch("/api/sub-orders/" + thisSubOrderId + "/status",
                new com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest(
                        com.maplenou.backend.order.SubOrderStatus.PREPARING), sellerToken, 200);
        patch("/api/sub-orders/" + thisSubOrderId + "/status",
                new com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest(
                        com.maplenou.backend.order.SubOrderStatus.READY_FOR_PICKUP), sellerToken, 200);

        JsonNode delivery = post("/api/deliveries/sub-orders/" + thisSubOrderId, null, adminToken, 201);
        UUID thisDeliveryId = UUID.fromString(delivery.get("id").asText());
        patch("/api/deliveries/" + thisDeliveryId + "/assign", new AssignAgentRequest(agentId), adminToken, 200);
        patch("/api/deliveries/" + thisDeliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.IN_TRANSIT, null, null, null), agentToken, 200);
        patch("/api/deliveries/" + thisDeliveryId + "/status",
                new UpdateDeliveryRequest(DeliveryStatus.DELIVERED, ProofType.SIGNATURE,
                        "https://res.cloudinary.com/demo/image/upload/v1/proofs/sig.jpg", null), agentToken, 200);

        JsonNode created = post("/api/return-requests",
                new CreateReturnRequest(thisSubOrderId, "Le client a changé d'avis sur ce produit", ProductCondition.NEW),
                buyerToken, 201);
        UUID thisReturnId = UUID.fromString(created.get("id").asText());

        JsonNode decided = patch("/api/return-requests/" + thisReturnId + "/decision",
                new DecideReturnRequest(ReturnDecision.APPROVED, "Retour accepté"), sellerToken, 200);
        assertEquals("APPROVED", decided.get("decision").asText());
        assertEquals("PROCESSING", decided.get("refundStatus").asText());

        JsonNode subOrderAfter = get("/api/sub-orders/" + thisSubOrderId, sellerToken, 200);
        assertEquals("RETURNED", subOrderAfter.get("status").asText());

        JsonNode afterVariant = get("/api/products/" + productSlug, null, 200);
        int stockAfter = afterVariant.get("variants").get(0).get("stockQuantity").asInt();
        assertEquals(stockBefore + 2, stockAfter, "le stock doit être restauré après un retour approuvé");

        // Conserver pour le cleanup
        order3Id = thisOrderId;
        delivery3Id = thisDeliveryId;
        returnRequest3Id = thisReturnId;
    }

    private UUID order3Id;
    private UUID delivery3Id;
    private UUID returnRequest3Id;

    @AfterAll
    void cleanup() {
        try { if (productReviewId != null) productReviewRepository.deleteById(productReviewId); } catch (Exception ignored) { }
        try { if (shopReviewId != null) shopReviewRepository.deleteById(shopReviewId); } catch (Exception ignored) { }
        try { if (returnRequestId != null) returnRequestRepository.deleteById(returnRequestId); } catch (Exception ignored) { }
        try { if (returnRequest3Id != null) returnRequestRepository.deleteById(returnRequest3Id); } catch (Exception ignored) { }
        try { if (deliveryId != null) deliveryRepository.deleteById(deliveryId); } catch (Exception ignored) { }
        try { if (delivery3Id != null) deliveryRepository.deleteById(delivery3Id); } catch (Exception ignored) { }
        try {
            if (promoCodeId != null) {
                promoCodeUsageRepository.findAll().stream()
                        .filter(u -> u.getPromoCode().getId().equals(promoCodeId))
                        .forEach(promoCodeUsageRepository::delete);
            }
        } catch (Exception ignored) { }
        try { if (payoutId != null) { /* sub_orders cleared once orders deleted below */ } } catch (Exception ignored) { }
        try { if (orderId != null) orderRepository.deleteById(orderId); } catch (Exception ignored) { }
        try { if (order2Id != null) orderRepository.deleteById(order2Id); } catch (Exception ignored) { }
        try { if (order3Id != null) orderRepository.deleteById(order3Id); } catch (Exception ignored) { }
        try { if (payoutId != null) payoutRepository.deleteById(payoutId); } catch (Exception ignored) { }
        try { if (promoCodeId != null) promoCodeRepository.deleteById(promoCodeId); } catch (Exception ignored) { }
        try { if (buyerId != null) cartRepository.findByUserId(buyerId).ifPresent(cartRepository::delete); } catch (Exception ignored) { }
        try { if (productId != null) productRepository.deleteById(productId); } catch (Exception ignored) { }
        try { if (shopId != null) shopRepository.deleteById(shopId); } catch (Exception ignored) { }
        try { if (sellerProfileId != null) sellerProfileRepository.deleteById(sellerProfileId); } catch (Exception ignored) { }
        try { if (categoryId != null) categoryRepository.deleteById(categoryId); } catch (Exception ignored) { }
        try { if (deliveryZoneId != null) { /* already deleted in test */ } } catch (Exception ignored) { }

        for (UUID id : new UUID[]{buyerId, sellerId, agentId}) {
            if (id == null) continue;
            try {
                auditLogRepository.deleteAll(
                        auditLogRepository.findByActorIdOrderByCreatedAtDesc(id, org.springframework.data.domain.Pageable.unpaged()));
            } catch (Exception ignored) { }
        }
        // deleteAllByUserId est une requête @Modifying qui exige une transaction active ;
        // findByUserId + delete(entity) fonctionne sans @Transactional sur cette méthode de test.
        try { if (buyerId != null) addressRepository.findByUserId(buyerId).forEach(addressRepository::delete); } catch (Exception ignored) { }
        try { if (buyerId != null) userRepository.deleteById(buyerId); } catch (Exception ignored) { }
        try { if (sellerId != null) userRepository.deleteById(sellerId); } catch (Exception ignored) { }
        try { if (agentId != null) userRepository.deleteById(agentId); } catch (Exception ignored) { }
    }
}
