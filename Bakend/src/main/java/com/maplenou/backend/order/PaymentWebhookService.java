package com.maplenou.backend.order;

import com.maplenou.backend.catalog.ProductVariantRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Traitement du webhook de paiement (FedaPay ou autre passerelle).
 * Appelé quand le paiement client est confirmé.
 *
 * Logique :
 *   1. Vérifier que la commande est en statut CREATED
 *   2. Décrémenter le stock atomiquement pour chaque article
 *   3. Passer la commande en PAID
 *   4. Si stock insuffisant sur un article → CANCELLED (cas de concurrence rare)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookService {

    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final ProductVariantRepository variantRepository;
    private final NotificationService notificationService;

    @Transactional
    public void handlePaymentConfirmed(UUID orderId, String paymentReference) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        // Idempotence : si déjà PAID, ignorer silencieusement
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Webhook reçu deux fois pour la commande {} — ignoré", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "La commande n'est pas en attente de paiement (statut actuel : " + order.getStatus() + ")");
        }

        // Décrémenter le stock atomiquement pour chaque article
        List<SubOrderItem> allItems = order.getSubOrders().stream()
                .flatMap(so -> so.getItems().stream())
                .toList();

        for (SubOrderItem item : allItems) {
            if (item.getProductVariant() == null) continue; // produit supprimé entre-temps

            int updated = variantRepository.decrementStock(
                    item.getProductVariant().getId(),
                    item.getQuantity()
            );

            if (updated == 0) {
                // Stock épuisé entre la validation du panier et le paiement (rare)
                log.warn("Stock insuffisant pour la variante {} — commande {} annulée",
                        item.getProductVariant().getId(), orderId);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                notificationService.orderCancelled(
                        order.getBuyer().getId(), order.getId(),
                        "Un article n'est plus disponible en stock.");
                return;
            }
        }

        // Tout le stock décrémenté → commande PAID
        order.setStatus(OrderStatus.PAID);
        order.setPaymentReference(paymentReference);
        orderRepository.save(order);

        // Notifier l'acheteur + chaque vendeur concerné
        notificationService.orderPaid(order.getBuyer().getId(), order.getId());
        order.getSubOrders().forEach(so ->
                notificationService.newSubOrderForSeller(
                        so.getShop().getOwner().getId(), so.getId(), so.getSubtotal()));

        log.info("Commande {} passée en PAID (réf. paiement : {})", orderId, paymentReference);
    }

    @Transactional
    public void handlePaymentFailed(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Commande introuvable"));

        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            log.info("Commande {} marquée PAYMENT_FAILED", orderId);
        }
    }
}
