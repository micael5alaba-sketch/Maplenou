package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import com.maplenou.backend.courier.CourierLocationRepository;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Affectation automatique des livraisons : à la mise "prêt" d'un colis, la livraison est créée et
 * affectée au livreur utilisable le plus proche de la boutique ; refus, réaffectation par l'admin
 * et cas sans livreur disponible.
 */
@TestPropertySource(properties = "app.dispatch.enabled=true")
class DeliveryDispatchIntegrationTest extends AbstractIntegrationTest {

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    private record Courier(String token, String id) {}

    @Autowired private CourierLocationRepository courierLocationRepository;

    private String adminToken;

    // Les positions de livreurs d'autres executions (base de dev partagee) fausseraient le "plus
    // proche" : on repart d'une table de positions vide, avant et apres ce test.
    @AfterAll
    void cleanLocations() {
        courierLocationRepository.deleteAll();
    }

    /** Crée un livreur dont les 4 documents sont validés et qui envoie sa position. */
    private Courier verifiedCourier(String name, long salt, Double lat, Double lng) throws Exception {
        String phone = "+22864" + String.format("%07d", (System.nanoTime() + salt) % 10_000_000L);
        JsonNode auth = post("/api/auth/register", Map.of("fullName", name, "phoneNumber", phone,
                "password", "Passw0rd!", "accountType", "DELIVERY_AGENT"), null, 201);
        String token = auth.get("accessToken").asText();
        for (String type : List.of("DRIVING_LICENSE", "ID_CARD", "VEHICLE_INSURANCE", "VEHICLE_REGISTRATION")) {
            JsonNode doc = post("/api/delivery-agents/me/documents", Map.of("type", type,
                    "fileUrl", "https://res.cloudinary.com/demo/image/upload/v1/docs/d.jpg"), token, 201);
            patch("/api/admin/courier-documents/" + doc.get("id").asText() + "/review", Map.of("decision", "VALID"), adminToken, 200);
        }
        if (lat != null) {
            exchange("PUT", "/api/delivery-agents/me/location", Map.of("latitude", lat, "longitude", lng), token, 204);
        }
        return new Courier(token, auth.get("user").get("id").asText());
    }

    @Test
    void nearestCourierGetsTheDeliveryAndCanBeReplaced() throws Exception {
        adminToken = login("+22890000000", "Micael2005@");
        courierLocationRepository.deleteAll();
        long rand = System.nanoTime() % 100_000_000L;

        // Boutique à Lomé centre (6.1725 / 1.2314)
        String sellerPhone = "+22865" + String.format("%07d", rand % 10_000_000L);
        JsonNode sellerAuth = post("/api/auth/register", Map.of("fullName", "Vendeur Dispatch", "phoneNumber", sellerPhone, "password", "Passw0rd!"), null, 201);
        JsonNode apply = post("/api/sellers/apply", Map.of("shopName", "Boutique Dispatch " + rand), sellerAuth.get("accessToken").asText(), 201);
        patch("/api/sellers/admin/" + apply.get("id").asText() + "/status", Map.of("status", "APPROVED", "reason", "ok"), adminToken, 200);
        String sellerToken = login(sellerPhone, "Passw0rd!");
        JsonNode shop = post("/api/shops", Map.of("name", "Boutique Dispatch " + rand, "description", "d", "city", "Lomé",
                "district", "Centre", "latitude", 6.1725, "longitude", 1.2314), sellerToken, 201);
        patch("/api/admin/shops/" + shop.get("id").asText() + "/status", Map.of("status", "APPROVED"), adminToken, 200);
        JsonNode category = post("/api/categories", Map.of("name", "Cat Dispatch " + rand), adminToken, 201);
        JsonNode product = post("/api/shops/mine/products", Map.of("name", "Produit Dispatch " + rand, "description", "d",
                "basePrice", 5000, "categoryId", category.get("id").asText(),
                "variants", List.of(Map.of("label", "Standard", "stockQuantity", 5, "sku", "SKU-DSP-" + rand))), sellerToken, 201);
        patch("/api/shops/mine/products/" + product.get("id").asText(), Map.of("status", "ACTIVE"), sellerToken, 200);

        // Livreurs : A proche (~1,1 km), B plus loin (~5 km), C trop loin (>15 km), D proche mais documents manquants
        Courier a = verifiedCourier("Livreur A", 1, 6.1825, 1.2314);
        Courier b = verifiedCourier("Livreur B", 2, 6.2175, 1.2314);
        Courier c = verifiedCourier("Livreur C", 3, 6.4000, 1.2314);
        String dPhone = "+22866" + String.format("%07d", rand % 10_000_000L);
        JsonNode dAuth = post("/api/auth/register", Map.of("fullName", "Livreur D", "phoneNumber", dPhone, "password", "Passw0rd!", "accountType", "DELIVERY_AGENT"), null, 201);
        exchange("PUT", "/api/delivery-agents/me/location", Map.of("latitude", 6.1730, "longitude", 1.2314), dAuth.get("accessToken").asText(), 204);

        // Commande payée, préparée puis marquée prête par le vendeur
        String buyerPhone = "+22867" + String.format("%07d", rand % 10_000_000L);
        JsonNode buyerAuth = post("/api/auth/register", Map.of("fullName", "Acheteur Dispatch", "phoneNumber", buyerPhone, "password", "Passw0rd!"), null, 201);
        String buyerToken = buyerAuth.get("accessToken").asText();
        JsonNode address = post("/api/users/me/addresses", Map.of("label", "Maison", "city", "Lomé", "district", "Bè",
                "details", "d", "latitude", 6.13, "longitude", 1.22, "isDefault", true), buyerToken, 201);
        post("/api/cart/items", Map.of("productVariantId", product.get("variants").get(0).get("id").asText(), "quantity", 1), buyerToken, 200);
        JsonNode order = post("/api/orders", Map.of("addressId", address.get("id").asText()), buyerToken, 201);
        postWithHeader("/api/webhooks/payment/confirmed", Map.of("orderId", order.get("id").asText(), "paymentReference", "PAY-DSP-" + rand),
                "X-Webhook-Secret", webhookSecret, 200);
        String subOrderId = order.get("subOrders").get(0).get("id").asText();
        patch("/api/sub-orders/" + subOrderId + "/status", Map.of("status", "PREPARING"), sellerToken, 200);
        patch("/api/sub-orders/" + subOrderId + "/status", Map.of("status", "READY_FOR_PICKUP"), sellerToken, 200);

        // 1) La livraison existe déjà et est affectée au livreur le plus proche utilisable : A
        JsonNode delivery = get("/api/deliveries/sub-orders/" + subOrderId, buyerToken, 200);
        String deliveryId = delivery.get("id").asText();
        assertEquals("ASSIGNED", delivery.get("status").asText());
        assertEquals(a.id(), delivery.get("agentId").asText(), "le livreur le plus proche doit être retenu");

        // 2) A refuse : la course passe à B (D n'a pas ses documents, C est hors du rayon)
        assertEquals(b.id(), patch("/api/deliveries/" + deliveryId + "/decline", null, a.token(), 200).get("agentId").asText());
        // Un autre livreur ne peut pas refuser la course d'un autre
        patch("/api/deliveries/" + deliveryId + "/decline", null, a.token(), 403);

        // 3) B refuse aussi : plus personne d'utilisable, la course attend (admin alerté par la pastille)
        JsonNode waiting = patch("/api/deliveries/" + deliveryId + "/decline", null, b.token(), 200);
        assertEquals("PENDING", waiting.get("status").asText());
        assertTrue(waiting.get("agentId").isNull());

        // 4) L'admin garde la main : affectation manuelle, puis réaffectation tant que la course n'a pas démarré
        assertEquals(c.id(), patch("/api/deliveries/" + deliveryId + "/assign", Map.of("agentId", c.id()), adminToken, 200).get("agentId").asText());
        JsonNode reassigned = patch("/api/deliveries/" + deliveryId + "/assign", Map.of("agentId", a.id()), adminToken, 200);
        assertEquals(a.id(), reassigned.get("agentId").asText());
        assertNotNull(reassigned.get("assignedAt"));
    }
}
