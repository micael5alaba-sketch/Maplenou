package com.maplenou.backend.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Façade de notifications — toutes les méthodes sont @Async.
 *
 * Les appelants passent uniquement des scalaires (UUID, String, BigDecimal),
 * JAMAIS des entités JPA (qui seraient détachées dans le thread async).
 *
 * Chaque méthode est fire-and-forget : les exceptions FCM sont loguées
 * et ne remontent jamais vers la transaction appelante.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FcmService fcmService;

    // ── Commandes ─────────────────────────────────────────────────────────────

    @Async
    public void orderPaid(UUID buyerId, UUID orderId) {
        try {
            fcmService.sendToUser(
                    buyerId,
                    "Commande confirmée !",
                    "Votre commande #" + shortId(orderId) + " est en cours de préparation.",
                    Map.of("type", "ORDER_PAID", "orderId", orderId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification orderPaid: {}", e.getMessage());
        }
    }

    @Async
    public void orderCancelled(UUID buyerId, UUID orderId, String reason) {
        try {
            fcmService.sendToUser(
                    buyerId,
                    "Commande annulée",
                    "Votre commande #" + shortId(orderId) + " a été annulée. " + reason,
                    Map.of("type", "ORDER_CANCELLED", "orderId", orderId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification orderCancelled: {}", e.getMessage());
        }
    }

    // ── Sous-commandes ────────────────────────────────────────────────────────

    @Async
    public void newSubOrderForSeller(UUID sellerId, UUID subOrderId, BigDecimal subtotal) {
        try {
            fcmService.sendToUser(
                    sellerId,
                    "Nouvelle commande !",
                    "Vous avez reçu une nouvelle commande de " + subtotal + " FCFA.",
                    Map.of("type", "NEW_SUB_ORDER", "subOrderId", subOrderId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification newSubOrderForSeller: {}", e.getMessage());
        }
    }

    @Async
    public void subOrderStatusChanged(UUID buyerId, UUID subOrderId, String status, String shopName) {
        try {
            String body = switch (status) {
                case "PREPARING"        -> "La boutique « " + shopName + " » prépare votre commande.";
                case "READY_FOR_PICKUP" -> "Votre commande chez « " + shopName + " » est prête pour le livreur.";
                case "IN_DELIVERY"      -> "Votre commande de « " + shopName + " » est en cours de livraison !";
                case "DELIVERED"        -> "Votre commande de « " + shopName + " » a été livrée. Bon shopping !";
                case "CANCELLED"        -> "Votre commande chez « " + shopName + " » a été annulée.";
                default                 -> null;
            };
            if (body == null) return;

            fcmService.sendToUser(
                    buyerId,
                    "Mise à jour de commande",
                    body,
                    Map.of("type", "SUB_ORDER_STATUS",
                           "subOrderId", subOrderId.toString(),
                           "status", status)
            );
        } catch (Exception e) {
            log.error("Erreur notification subOrderStatusChanged: {}", e.getMessage());
        }
    }

    // ── Livraisons ────────────────────────────────────────────────────────────

    @Async
    public void deliveryAssigned(UUID agentId, UUID deliveryId, String shopName) {
        try {
            fcmService.sendToUser(
                    agentId,
                    "Nouvelle livraison assignée",
                    "Une livraison depuis la boutique « " + shopName + " » vous a été attribuée.",
                    Map.of("type", "DELIVERY_ASSIGNED", "deliveryId", deliveryId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification deliveryAssigned: {}", e.getMessage());
        }
    }

    // ── Retours ───────────────────────────────────────────────────────────────

    @Async
    public void returnRequestCreated(UUID sellerId, UUID returnId) {
        try {
            fcmService.sendToUser(
                    sellerId,
                    "Demande de retour reçue",
                    "Un acheteur a soumis une demande de retour pour une commande de votre boutique.",
                    Map.of("type", "RETURN_CREATED", "returnId", returnId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification returnRequestCreated: {}", e.getMessage());
        }
    }

    @Async
    public void returnDecided(UUID buyerId, UUID returnId, String decision) {
        try {
            boolean approved = "APPROVED".equals(decision);
            fcmService.sendToUser(
                    buyerId,
                    approved ? "Retour approuvé" : "Retour refusé",
                    approved
                        ? "Votre demande de retour a été approuvée. Le remboursement est en cours."
                        : "Votre demande de retour a été refusée.",
                    Map.of("type", "RETURN_DECIDED", "returnId", returnId.toString(), "decision", decision)
            );
        } catch (Exception e) {
            log.error("Erreur notification returnDecided: {}", e.getMessage());
        }
    }

    // ── Boutique ──────────────────────────────────────────────────────────────

    @Async
    public void shopStatusChanged(UUID ownerId, String shopName, String newStatus) {
        try {
            boolean approved = "APPROVED".equals(newStatus);
            fcmService.sendToUser(
                    ownerId,
                    approved ? "Boutique approuvée !" : "Statut de boutique mis à jour",
                    approved
                        ? "Votre boutique « " + shopName + " » a été approuvée. Vous pouvez commencer à vendre !"
                        : "Le statut de votre boutique « " + shopName + " » est maintenant : " + newStatus,
                    Map.of("type", "SHOP_STATUS", "status", newStatus)
            );
        } catch (Exception e) {
            log.error("Erreur notification shopStatusChanged: {}", e.getMessage());
        }
    }

    // ── Messagerie ────────────────────────────────────────────────────────────

    @Async
    public void newMessage(UUID recipientId, UUID conversationId, String senderName) {
        try {
            fcmService.sendToUser(
                    recipientId,
                    "Nouveau message de " + senderName,
                    "Vous avez reçu un nouveau message.",
                    Map.of("type", "NEW_MESSAGE", "conversationId", conversationId.toString())
            );
        } catch (Exception e) {
            log.error("Erreur notification newMessage: {}", e.getMessage());
        }
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    private String shortId(UUID id) {
        return id.toString().substring(0, 8).toUpperCase();
    }
}
