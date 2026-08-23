package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.catalog.dto.CreateShopRequest;
import com.maplenou.backend.content.ContentPageRepository;
import com.maplenou.backend.content.dto.CreateContentPageRequest;
import com.maplenou.backend.content.dto.UpdateContentPageRequest;
import com.maplenou.backend.messaging.ConversationRepository;
import com.maplenou.backend.messaging.MessageRepository;
import com.maplenou.backend.messaging.dto.SendMessageRequest;
import com.maplenou.backend.newsletter.NewsletterSubscriberRepository;
import com.maplenou.backend.newsletter.dto.SubscribeRequest;
import com.maplenou.backend.seller.SellerProfileRepository;
import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
import com.maplenou.backend.user.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test d'intégration de bout en bout pour les 4 modules ajoutés le 2026-08-22 :
 * Cloudinary (media), messagerie, pages de contenu (CMS) et transporteur tiers (shipping),
 * plus la newsletter.
 */
class NewFeaturesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShopRepository shopRepository;
    @Autowired
    private SellerProfileRepository sellerProfileRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private ContentPageRepository contentPageRepository;
    @Autowired
    private NewsletterSubscriberRepository newsletterSubscriberRepository;

    private final long rand = System.currentTimeMillis() % 100_000_000L;

    private UUID buyerId;
    private UUID sellerId;
    private UUID sellerProfileId;
    private UUID shopId;
    private UUID conversationId;
    private UUID supportConversationId;
    private UUID pageId;
    private final String subscriberEmail = "junit-test-" + rand + "@example.com";

    @Test
    void fullBackendFeaturesFlow() throws Exception {
        String buyerPhone = "+22890" + String.format("%07d", rand % 10_000_000L);
        String sellerPhone = "+22891" + String.format("%07d", rand % 10_000_000L);

        // ----- Auth -----
        String adminToken = login("+22890000000", "Micael2005@");

        JsonNode buyerAuth = post("/api/auth/register",
                new RegisterRequest("Client JUnit", buyerPhone, "Passw0rd!"), null, 201);
        String buyerToken = buyerAuth.get("accessToken").asText();
        buyerId = UUID.fromString(buyerAuth.get("user").get("id").asText());

        JsonNode sellerAuth = post("/api/auth/register",
                new RegisterRequest("Vendeur JUnit", sellerPhone, "Passw0rd!"), null, 201);
        sellerId = UUID.fromString(sellerAuth.get("user").get("id").asText());

        // ----- Seller onboarding -----
        String sellerTokenBeforeApproval = sellerAuth.get("accessToken").asText();
        JsonNode apply = post("/api/sellers/apply",
                new ApplySellerRequest("Boutique JUnit " + rand), sellerTokenBeforeApproval, 201);
        sellerProfileId = UUID.fromString(apply.get("id").asText());

        JsonNode approved = patch("/api/sellers/admin/" + sellerProfileId + "/status",
                new UpdateSellerStatusRequest(SellerStatus.APPROVED, "ok"), adminToken, 200);
        assertEquals("APPROVED", approved.get("status").asText());

        // Reconnexion nécessaire : ROLE_SELLER est dérivé au login, pas rafraîchi sur un token existant.
        String sellerToken = login(sellerPhone, "Passw0rd!");

        JsonNode shop = post("/api/shops",
                new CreateShopRequest("Boutique JUnit " + rand, "desc", "Lomé", "Centre"), sellerToken, 201);
        shopId = UUID.fromString(shop.get("id").asText());

        // ----- Newsletter -----
        post("/api/newsletter/subscribe", new SubscribeRequest(subscriberEmail), null, 201);
        post("/api/newsletter/subscribe", new SubscribeRequest(subscriberEmail), null, 409);
        JsonNode subscribers = get("/api/admin/newsletter/subscribers", adminToken, 200);
        assertTrue(subscribers.get("content").size() >= 1, "au moins un abonné actif");
        post("/api/newsletter/unsubscribe", new SubscribeRequest(subscriberEmail), null, 204);

        // ----- CMS pages -----
        String slug = "cgu-junit-" + rand;
        JsonNode createdPage = post("/api/admin/pages",
                new CreateContentPageRequest(slug, "CGU", "Contenu des CGU", true), adminToken, 201);
        pageId = UUID.fromString(createdPage.get("id").asText());

        JsonNode publicPage = get("/api/pages/" + slug, null, 200);
        assertEquals("CGU", publicPage.get("title").asText());

        put("/api/admin/pages/" + pageId,
                new UpdateContentPageRequest("CGU v2", "Contenu v2", false), adminToken, 200);
        get("/api/pages/" + slug, null, 404); // dépubliée -> invisible au public

        delete("/api/admin/pages/" + pageId, adminToken, 204);
        pageId = null; // déjà supprimée, pas besoin de cleanup

        // ----- Messagerie boutique <-> client -----
        JsonNode conv = post("/api/conversations/shops/" + shopId, null, buyerToken, 201);
        conversationId = UUID.fromString(conv.get("id").asText());

        JsonNode convAgain = post("/api/conversations/shops/" + shopId, null, buyerToken, 201);
        assertEquals(conversationId.toString(), convAgain.get("id").asText(), "même conversation réutilisée");

        post("/api/conversations/" + conversationId + "/messages",
                new SendMessageRequest("Bonjour, le produit est-il disponible ?"), buyerToken, 201);

        post("/api/conversations/" + conversationId + "/messages",
                new SendMessageRequest("Appelle-moi au 90 12 34 56 stp"), buyerToken, 400);
        post("/api/conversations/" + conversationId + "/messages",
                new SendMessageRequest("Contacte-moi sur whatsapp"), buyerToken, 400);

        post("/api/conversations/" + conversationId + "/messages",
                new SendMessageRequest("Oui il est disponible !"), sellerToken, 201);

        JsonNode messages = get("/api/conversations/" + conversationId + "/messages", buyerToken, 200);
        assertEquals(2, messages.get("content").size(), "les messages filtrés ne sont jamais enregistrés");

        get("/api/conversations/" + conversationId + "/messages", adminToken, 403);
        patch("/api/conversations/" + conversationId + "/read", null, sellerToken, 204);

        JsonNode mine = get("/api/conversations", buyerToken, 200);
        assertTrue(mine.get("content").size() >= 1);

        // ----- Messagerie support -----
        JsonNode support = post("/api/conversations/support", null, buyerToken, 201);
        supportConversationId = UUID.fromString(support.get("id").asText());

        // Le filtre anti-coordonnées ne s'applique qu'aux conversations BUYER_SELLER, pas SUPPORT.
        post("/api/conversations/" + supportConversationId + "/messages",
                new SendMessageRequest("Mon numero est 90112233"), buyerToken, 201);

        JsonNode supportList = get("/api/admin/conversations/support", adminToken, 200);
        assertTrue(supportList.get("content").size() >= 1);
        get("/api/conversations/" + supportConversationId + "/messages", adminToken, 200);

        // ----- Media (Cloudinary non configuré en dev -> 503 attendu, pas une 500) -----
        post("/api/media/upload-signature", null, buyerToken, 503);

        // ----- Shipping (Colissimo désactivé par défaut -> erreurs contrôlées, pas de crash) -----
        String fakeSubOrderId = UUID.randomUUID().toString();
        post("/api/shops/mine/sub-orders/" + fakeSubOrderId + "/shipping/label", null, sellerToken, 404);
        post("/api/shops/mine/sub-orders/" + fakeSubOrderId + "/shipping/label", null, buyerToken, 403);
    }

    @AfterAll
    void cleanup() {
        try {
            if (conversationId != null) {
                messageRepository.deleteAll(messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, Pageable.unpaged()));
                conversationRepository.deleteById(conversationId);
            }
        } catch (Exception ignored) { }
        try {
            if (supportConversationId != null) {
                messageRepository.deleteAll(messageRepository.findByConversationIdOrderByCreatedAtDesc(supportConversationId, Pageable.unpaged()));
                conversationRepository.deleteById(supportConversationId);
            }
        } catch (Exception ignored) { }
        try { if (pageId != null) contentPageRepository.deleteById(pageId); } catch (Exception ignored) { }
        try { newsletterSubscriberRepository.findByEmail(subscriberEmail).ifPresent(newsletterSubscriberRepository::delete); } catch (Exception ignored) { }
        try { if (shopId != null) shopRepository.deleteById(shopId); } catch (Exception ignored) { }
        try { if (sellerProfileId != null) sellerProfileRepository.deleteById(sellerProfileId); } catch (Exception ignored) { }
        try { if (buyerId != null) userRepository.deleteById(buyerId); } catch (Exception ignored) { }
        try { if (sellerId != null) userRepository.deleteById(sellerId); } catch (Exception ignored) { }
    }
}
