package com.maplenou.backend.supportbridge;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.messaging.MessagingService;
import com.maplenou.backend.messaging.dto.ConversationResponse;
import com.maplenou.backend.messaging.dto.SendMessageRequest;
import com.maplenou.backend.notification.FcmService;
import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.returns.ProductCondition;
import com.maplenou.backend.returns.ReturnRequestService;
import com.maplenou.backend.returns.dto.CreateReturnRequest;
import com.maplenou.backend.returns.dto.ReturnRequestResponse;
import com.maplenou.backend.supportbridge.SupportBridgeDtos.*;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Les seules écritures que le Service client peut demander à Bakend (DEC-CS11-07), toujours depuis un ticket
 * et au nom d'un agent : ouvrir une demande de retour pour l'acheteur, remettre une livraison à disposition des
 * livreurs, écrire au livreur dans la messagerie support, et relayer une notification push vers l'app mobile.
 *
 * Chaque action passe par le service métier existant (mêmes règles que dans l'app : fenêtre de 30 jours, un seul
 * retour par sous-commande…) et laisse une trace dans {@code AuditLog} avec le numéro du ticket.
 */
@Slf4j
@Service
public class SupportBridgeActionService {

    private final EntityManager em;
    private final ReturnRequestService returns;
    private final MessagingService messaging;
    private final FcmService fcm;
    private final AuditService audit;
    private final String senderUserId;

    public SupportBridgeActionService(EntityManager em, ReturnRequestService returns, MessagingService messaging,
                                      FcmService fcm, AuditService audit,
                                      @Value("${app.support-bridge.sender-user-id:}") String senderUserId) {
        this.em = em;
        this.returns = returns;
        this.messaging = messaging;
        this.fcm = fcm;
        this.audit = audit;
        this.senderUserId = senderUserId;
    }

    // ---------------------------------------------------------------- retour / remboursement

    @Transactional
    public ActionResult requestReturn(String orderNumber, ReturnRequestAction req) {
        Order order = em.createQuery("SELECT o FROM Order o WHERE o.orderNumber = :n", Order.class)
                .setParameter("n", orderNumber == null ? "" : orderNumber.trim())
                .getResultStream().findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Commande introuvable"));
        SubOrder sub = pickSubOrder(order, req.subOrderId());
        ProductCondition condition = req.productCondition() == null || req.productCondition().isBlank()
                ? ProductCondition.GOOD : ProductCondition.valueOf(req.productCondition());
        String reason = prefix(req.origin()) + req.reason();
        ReturnRequestResponse created = returns.create(order.getBuyer(),
                new CreateReturnRequest(sub.getId(), reason, condition));
        log(req.origin(), AuditAction.SUPPORT_RETURN_REQUESTED, "ReturnRequest", created.id(),
                "Commande " + order.getOrderNumber() + ", sous-commande " + sub.getId());
        return new ActionResult(created.id().toString(), created.refundStatus().name());
    }

    private static SubOrder pickSubOrder(Order order, String subOrderId) {
        List<SubOrder> subs = order.getSubOrders();
        if (subOrderId == null || subOrderId.isBlank()) {
            if (subs.size() != 1) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "La commande contient " + subs.size() + " boutiques : précisez la sous-commande concernée.");
            }
            return subs.get(0);
        }
        return subs.stream().filter(s -> s.getId().toString().equals(subOrderId.trim())).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Cette sous-commande n'appartient pas à la commande"));
    }

    // ---------------------------------------------------------------- livraison

    /**
     * Retire la course au livreur actuel (ASSIGNED, ou FAILED après un échec) et la remet dans la liste des courses
     * disponibles (PENDING). Une course en route (IN_TRANSIT) ou livrée ne peut pas être retirée.
     */
    @Transactional
    public ActionResult rescheduleDelivery(String deliveryId, RescheduleAction req) {
        UUID id = parseUuid(deliveryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
        Delivery delivery = Optional.ofNullable(em.find(Delivery.class, id, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
        DeliveryStatus status = delivery.getStatus();
        if (status != DeliveryStatus.ASSIGNED && status != DeliveryStatus.FAILED) {
            throw new ApiException(HttpStatus.CONFLICT, switch (status) {
                case PENDING -> "La livraison attend déjà un livreur.";
                case IN_TRANSIT -> "Le livreur est en route : la course ne peut pas lui être retirée.";
                default -> "La livraison est terminée.";
            });
        }
        User previous = delivery.getAgent();
        delivery.setAgent(null);
        delivery.setAssignedAt(null);
        delivery.setStatus(DeliveryStatus.PENDING);
        String note = prefix(req.origin()) + req.reason();
        String notes = delivery.getNotes() == null || delivery.getNotes().isBlank() ? note : delivery.getNotes() + "\n" + note;
        delivery.setNotes(notes.length() > 1000 ? notes.substring(notes.length() - 1000) : notes);
        SubOrder sub = delivery.getSubOrder();
        if (sub.getStatus() == SubOrderStatus.IN_DELIVERY) {
            sub.setStatus(SubOrderStatus.READY_FOR_PICKUP);
        }
        log(req.origin(), AuditAction.SUPPORT_DELIVERY_RESCHEDULED, "Delivery", delivery.getId(),
                "Statut " + status + " -> PENDING" + (previous == null ? "" : ", livreur " + previous.getId()));
        if (previous != null) {
            pushQuietly(previous.getId(), "Course retirée",
                    "Le service client a remis une de vos courses à disposition des autres livreurs.",
                    Map.of("type", "DELIVERY_UNASSIGNED", "deliveryId", delivery.getId().toString()));
        }
        return new ActionResult(delivery.getId().toString(), DeliveryStatus.PENDING.name());
    }

    // ---------------------------------------------------------------- message au livreur

    @Transactional
    public ActionResult messageDriver(UUID driverId, DriverMessageAction req) {
        User driver = em.find(User.class, driverId);
        if (driver == null || driver.getRole() != Role.DELIVERY_AGENT || !driver.isActive()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Livreur introuvable");
        }
        User sender = supportSender();
        ConversationResponse conversation = messaging.startOrGetSupportConversation(driver);
        String agent = req.agentName() == null || req.agentName().isBlank() ? "" : " (" + req.agentName() + ")";
        String content = "Service client" + agent + (req.ticketNumber() == null ? "" : " — ticket " + req.ticketNumber())
                + " :\n" + req.message();
        messaging.sendMessage(sender, conversation.id(), new SendMessageRequest(
                content.length() > 2000 ? content.substring(0, 2000) : content));
        log(req.origin(), AuditAction.SUPPORT_DRIVER_MESSAGED, "User", driver.getId(), "Conversation " + conversation.id());
        return new ActionResult(conversation.id().toString(), "SENT");
    }

    /** Compte qui signe les messages du support : {@code SUPPORT_SENDER_USER_ID}, sinon le plus ancien admin actif. */
    private User supportSender() {
        Optional<User> configured = parseUuid(senderUserId).map(id -> em.find(User.class, id))
                .filter(u -> u.getRole() == Role.ADMIN && u.isActive());
        return configured.or(() -> em.createQuery(
                        "SELECT u FROM User u WHERE u.role = :r AND u.active = true ORDER BY u.createdAt", User.class)
                        .setParameter("r", Role.ADMIN).setMaxResults(1).getResultStream().findFirst())
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "Aucun compte administrateur actif pour signer le message du service client."));
    }

    // ---------------------------------------------------------------- push relayé

    /** Notification du Service client vers l'app mobile Maplenou (jetons FCM gérés par Bakend). */
    public void relayPush(UUID userId, PushRelay req) {
        if (em.find(User.class, userId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable");
        }
        Map<String, String> data = new HashMap<>(req.data() == null ? Map.of() : req.data());
        data.put("source", "support");
        fcm.sendToUser(userId, req.title(), req.body(), data);
    }

    private void pushQuietly(UUID userId, String title, String body, Map<String, String> data) {
        try {
            fcm.sendToUser(userId, title, body, data);
        } catch (RuntimeException e) {
            log.warn("Notification push non envoyée à {} : {}", userId, e.getMessage());
        }
    }

    // ---------------------------------------------------------------- outils

    private static String prefix(ActionOrigin origin) {
        return origin.ticketNumber() == null || origin.ticketNumber().isBlank()
                ? "[Service client] " : "[Service client " + origin.ticketNumber() + "] ";
    }

    /** L'agent n'a pas forcément de compte Bakend (compte de démonstration) : l'auteur reste alors vide. */
    private void log(ActionOrigin origin, AuditAction action, String type, UUID entityId, String details) {
        UUID actor = parseUuid(origin.agentId()).filter(id -> em.find(User.class, id) != null).orElse(null);
        String who = "Ticket " + origin.ticketNumber() + ", agent " + origin.agentName() + ". " + details;
        audit.log(actor, action, type, entityId, who.length() > 1000 ? who.substring(0, 1000) : who, null);
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
