package com.maplenou.backend.supportbridge;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Réponses de l'API interne lue par le Service client. Les noms de champs sont ceux attendus par
 * {@code ServiceClient/backend/.../context/EcosystemGateway} : ne pas les renommer sans changer les deux côtés.
 */
public final class SupportBridgeDtos {

    private SupportBridgeDtos() {
    }

    /** Une commande (référence = numéro de commande). Vendeur renseigné quand la commande n'a qu'une boutique. */
    public record OrderSummary(String ref, UUID customerId, String customerName, UUID sellerUserId, String sellerRef,
                               String sellerName, Instant orderedAt, BigDecimal amount, String currency, String status,
                               String paymentRef, String deliveryRef, String refundRef,
                               List<SubOrderSummary> subOrders) {
    }

    /** Une sous-commande (une par boutique) : le Service client choisit celle concernée par un retour. */
    public record SubOrderSummary(String id, String shopName, String status, BigDecimal subtotal) {
    }

    public record PaymentSummary(String ref, String orderRef, String status, BigDecimal amount, String currency,
                                 String provider, BigDecimal commissionAmount, String payoutStatus,
                                 BigDecimal payoutAmount, Instant paidAt) {
    }

    public record DeliverySummary(String ref, String orderRef, String customerName, UUID driverId, String driverName,
                                  String status, Instant pickedUpAt, Instant lastUpdateAt, String destination,
                                  String timeSlot) {
    }

    public record SellerSummary(String ref, UUID sellerUserId, String shopName, String ownerName,
                                String accountStatus, BigDecimal balance, String currency) {
    }

    public record RefundSummary(String ref, String orderRef, String status, BigDecimal amount, String currency,
                                Instant requestedAt) {
    }

    /** Ancien ticket (table support_tickets, V46), pour l'import unique dans le Service client. */
    public record LegacyTicket(UUID id, String ticketNumber, UUID requesterId, String requesterName,
                               String requesterPhone, String requesterEmail, String requesterRole, String category,
                               String subject, String description, String relatedDeliveryRef, String photoUrl,
                               String status, String resolutionNotes, Instant resolvedAt, Instant createdAt) {
    }

    public record LegacyTicketPage(List<LegacyTicket> items, int page, int size, long totalItems, boolean last) {
    }

    // ---------------------------------------------------------------- actions demandées par le Service client

    /** Commun à toutes les actions : qui la demande, depuis quel ticket (traçabilité, AuditLog). */
    public record ActionOrigin(String ticketNumber, String agentName, String agentId) {
    }

    public record ReturnRequestAction(
            @jakarta.validation.constraints.Size(max = 40) String ticketNumber,
            @jakarta.validation.constraints.Size(max = 120) String agentName,
            @jakarta.validation.constraints.Size(max = 40) String agentId,
            @jakarta.validation.constraints.Size(max = 60) String subOrderId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 10, max = 900) String reason,
            @jakarta.validation.constraints.Pattern(regexp = "^(NEW|GOOD|DAMAGED)?$") String productCondition) {
        ActionOrigin origin() {
            return new ActionOrigin(ticketNumber, agentName, agentId);
        }
    }

    public record RescheduleAction(
            @jakarta.validation.constraints.Size(max = 40) String ticketNumber,
            @jakarta.validation.constraints.Size(max = 120) String agentName,
            @jakarta.validation.constraints.Size(max = 40) String agentId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 500) String reason) {
        ActionOrigin origin() {
            return new ActionOrigin(ticketNumber, agentName, agentId);
        }
    }

    public record DriverMessageAction(
            @jakarta.validation.constraints.Size(max = 40) String ticketNumber,
            @jakarta.validation.constraints.Size(max = 120) String agentName,
            @jakarta.validation.constraints.Size(max = 40) String agentId,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 1000) String message) {
        ActionOrigin origin() {
            return new ActionOrigin(ticketNumber, agentName, agentId);
        }
    }

    public record PushRelay(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String title,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 500) String body,
            @jakarta.validation.constraints.Size(max = 10) java.util.Map<String, String> data) {
    }

    /** Résultat d'une action : référence créée côté Bakend (retour, livraison…) et son statut. */
    public record ActionResult(String ref, String status) {
    }
}
