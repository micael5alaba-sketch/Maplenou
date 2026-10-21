package com.maplenou.backend.notification;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.dto.NotificationResponse;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Façade de notifications — toutes les méthodes sont @Async.
 *
 * Les appelants passent uniquement des scalaires (UUID, String, BigDecimal),
 * JAMAIS des entités JPA (qui seraient détachées dans le thread async).
 *
 * Chaque méthode est fire-and-forget : les exceptions FCM et de persistance sont loguées
 * et ne remontent jamais vers la transaction appelante. Persistée systématiquement en plus
 * de l'envoi push (voir {@link AppNotification}) — sans ça, rien ne reste consultable après
 * réinstallation de l'app, sur un second appareil, ou si les push sont refusés.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FcmService fcmService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Commandes ─────────────────────────────────────────────────────────────

    @Async
    public void orderPaid(UUID buyerId, UUID orderId) {
        String title = "Commande confirmée !";
        String body = "Votre commande #" + shortId(orderId) + " est en cours de préparation.";
        send(buyerId, title, body, "ORDER_PAID", orderId.toString(),
                Map.of("type", "ORDER_PAID", "orderId", orderId.toString()));
    }

    @Async
    public void orderCancelled(UUID buyerId, UUID orderId, String reason) {
        String title = "Commande annulée";
        String body = "Votre commande #" + shortId(orderId) + " a été annulée. " + reason;
        send(buyerId, title, body, "ORDER_CANCELLED", orderId.toString(),
                Map.of("type", "ORDER_CANCELLED", "orderId", orderId.toString()));
    }

    // ── Sous-commandes ────────────────────────────────────────────────────────

    @Async
    public void newSubOrderForSeller(UUID sellerId, UUID subOrderId, BigDecimal subtotal) {
        String title = "Nouvelle commande !";
        String body = "Vous avez reçu une nouvelle commande de " + subtotal + " FCFA.";
        send(sellerId, title, body, "NEW_SUB_ORDER", subOrderId.toString(),
                Map.of("type", "NEW_SUB_ORDER", "subOrderId", subOrderId.toString()));
    }

    @Async
    public void subOrderStatusChanged(UUID buyerId, UUID subOrderId, String status, String shopName) {
        String body = switch (status) {
            case "PREPARING"        -> "La boutique « " + shopName + " » prépare votre commande.";
            case "READY_FOR_PICKUP" -> "Votre commande chez « " + shopName + " » est prête pour le livreur.";
            case "IN_DELIVERY"      -> "Votre commande de « " + shopName + " » est en cours de livraison !";
            case "DELIVERED"        -> "Votre commande de « " + shopName + " » a été livrée. Bon shopping !";
            case "CANCELLED"        -> "Votre commande chez « " + shopName + " » a été annulée.";
            default                 -> null;
        };
        if (body == null) return;
        send(buyerId, "Mise à jour de commande", body, "SUB_ORDER_STATUS", subOrderId.toString(),
                Map.of("type", "SUB_ORDER_STATUS", "subOrderId", subOrderId.toString(), "status", status));
    }

    // ── Livraisons ────────────────────────────────────────────────────────────

    @Async
    public void deliveryAssigned(UUID agentId, UUID deliveryId, String shopName) {
        String body = "Une livraison depuis la boutique « " + shopName + " » vous a été attribuée.";
        send(agentId, "Nouvelle livraison assignée", body, "DELIVERY_ASSIGNED", deliveryId.toString(),
                Map.of("type", "DELIVERY_ASSIGNED", "deliveryId", deliveryId.toString()));
    }

    // ── Retours ───────────────────────────────────────────────────────────────

    @Async
    public void returnRequestCreated(UUID sellerId, UUID returnId) {
        String body = "Un acheteur a soumis une demande de retour pour une commande de votre boutique.";
        send(sellerId, "Demande de retour reçue", body, "RETURN_CREATED", returnId.toString(),
                Map.of("type", "RETURN_CREATED", "returnId", returnId.toString()));
    }

    @Async
    public void returnDecided(UUID buyerId, UUID returnId, String decision) {
        boolean approved = "APPROVED".equals(decision);
        String title = approved ? "Retour approuvé" : "Retour refusé";
        String body = approved
                ? "Votre demande de retour a été approuvée. Le remboursement est en cours."
                : "Votre demande de retour a été refusée.";
        send(buyerId, title, body, "RETURN_DECIDED", returnId.toString(),
                Map.of("type", "RETURN_DECIDED", "returnId", returnId.toString(), "decision", decision));
    }

    // ── Boutique ──────────────────────────────────────────────────────────────

    @Async
    public void shopStatusChanged(UUID ownerId, String shopName, String newStatus) {
        boolean approved = "APPROVED".equals(newStatus);
        String title = approved ? "Boutique approuvée !" : "Statut de boutique mis à jour";
        String body = approved
                ? "Votre boutique « " + shopName + " » a été approuvée. Vous pouvez commencer à vendre !"
                : "Le statut de votre boutique « " + shopName + " » est maintenant : " + newStatus;
        send(ownerId, title, body, "SHOP_STATUS", null, Map.of("type", "SHOP_STATUS", "status", newStatus));
    }

    // ── Messagerie ────────────────────────────────────────────────────────────

    @Async
    public void newMessage(UUID recipientId, UUID conversationId, String senderName) {
        send(recipientId, "Nouveau message de " + senderName, "Vous avez reçu un nouveau message.",
                "NEW_MESSAGE", conversationId.toString(),
                Map.of("type", "NEW_MESSAGE", "conversationId", conversationId.toString()));
    }

    // ── Frais vendeur ─────────────────────────────────────────────────────────

    @Async
    public void sellerFeeActivated(UUID sellerId, BigDecimal amount, String categoryLabel, String monthLabel) {
        String body = "Votre catégorie « " + categoryLabel + " » entraîne un frais de " + amount
                + " FCFA pour " + monthLabel + ", déduit avant votre prochain reversement.";
        send(sellerId, "Frais mensuel d'activité", body, "SELLER_FEE_ACTIVATED", null,
                Map.of("type", "SELLER_FEE_ACTIVATED"));
    }

    @Async
    public void sellerFeeDeducted(UUID sellerId, BigDecimal amount) {
        String body = amount + " FCFA de frais d'activité ont été déduits de votre dernier reversement.";
        send(sellerId, "Frais déduit de votre reversement", body, "SELLER_FEE_DEDUCTED", null,
                Map.of("type", "SELLER_FEE_DEDUCTED"));
    }

    // ── Consultation (écran "Notifications" de l'app) ───────────────────────────

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listMine(User user, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(User user) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markAsRead(User user, UUID notificationId) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification introuvable"));
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette notification ne vous appartient pas");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user.getId(), Instant.now());
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    /** Envoie le push ET persiste la notification, chacun indépendamment protégé : un échec de
     * l'un ne doit jamais empêcher l'autre (ex: FCM injoignable ne doit pas priver l'utilisateur
     * de l'historique consultable dans l'app). */
    private void send(UUID recipientId, String title, String body, String type, String referenceId,
                       Map<String, String> fcmData) {
        try {
            fcmService.sendToUser(recipientId, title, body, fcmData);
        } catch (Exception e) {
            log.error("Erreur envoi push ({}): {}", type, e.getMessage());
        }
        persist(recipientId, title, body, type, referenceId);
    }

    private void persist(UUID recipientId, String title, String body, String type, String referenceId) {
        try {
            notificationRepository.save(AppNotification.builder()
                    .recipient(userRepository.getReferenceById(recipientId))
                    .title(title)
                    .body(body)
                    .type(type)
                    .referenceId(referenceId)
                    .build());
        } catch (Exception e) {
            log.error("Erreur persistance notification ({}): {}", type, e.getMessage());
        }
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }
}
