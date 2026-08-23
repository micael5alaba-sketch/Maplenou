package com.maplenou.backend.returns;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.ProductVariantRepository;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.returns.dto.CreateReturnRequest;
import com.maplenou.backend.returns.dto.DecideReturnRequest;
import com.maplenou.backend.returns.dto.ReturnRequestResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnRequestRepository returnRepository;
    private final SubOrderRepository subOrderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ProductVariantRepository variantRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final int RETURN_WINDOW_DAYS = 30;

    // ── Acheteur ──────────────────────────────────────────────────────────────

    /**
     * L'acheteur crée une demande de retour.
     * Règles :
     *   - La sous-commande doit être DELIVERED
     *   - La demande doit être faite dans les 30 jours après livraison
     *   - Un seul retour par sous-commande
     *   - L'acheteur doit être le propriétaire de la commande
     */
    @Transactional
    public ReturnRequestResponse create(User buyer, CreateReturnRequest request) {

        SubOrder subOrder = subOrderRepository.findByIdWithDetails(request.subOrderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        // Vérification d'ownership : la commande appartient bien à cet acheteur
        if (!subOrder.getOrder().getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Cette sous-commande ne vous appartient pas");
        }

        // La sous-commande doit être livrée
        if (subOrder.getStatus() != SubOrderStatus.DELIVERED) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Un retour n'est possible que sur une sous-commande livrée (statut actuel : "
                    + subOrder.getStatus() + ")");
        }

        // Un seul retour par sous-commande
        if (returnRepository.existsBySubOrderId(subOrder.getId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Une demande de retour existe déjà pour cette sous-commande");
        }

        // Récupérer la date de livraison depuis la Delivery
        Delivery delivery = deliveryRepository.findBySubOrderId(subOrder.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Livraison introuvable pour cette sous-commande"));

        if (delivery.getDeliveredAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La date de livraison n'est pas encore confirmée");
        }

        Instant deliveryDate   = delivery.getDeliveredAt();
        Instant returnDeadline = deliveryDate.plus(RETURN_WINDOW_DAYS, ChronoUnit.DAYS);

        // Vérification de la fenêtre de retour (30 jours)
        if (Instant.now().isAfter(returnDeadline)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le délai de retour de " + RETURN_WINDOW_DAYS
                    + " jours après livraison est dépassé");
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .subOrder(subOrder)
                .buyer(buyer)
                .deliveryDate(deliveryDate)
                .returnDeadline(returnDeadline)
                .reason(request.reason())
                .productCondition(request.productCondition())
                .refundStatus(RefundStatus.PENDING)
                .build();

        // Passer le sous-ordre en RETURN_REQUESTED
        subOrder.setStatus(SubOrderStatus.RETURN_REQUESTED);
        subOrderRepository.save(subOrder);

        ReturnRequest saved = returnRepository.save(returnRequest);
        notificationService.returnRequestCreated(
                subOrder.getShop().getOwner().getId(), saved.getId());
        return reload(saved.getId());
    }

    /** Acheteur : liste mes demandes de retour. */
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listMine(User buyer, Pageable pageable) {
        return returnRepository
                .findByBuyerIdWithDetails(buyer.getId(), pageable)
                .map(ReturnRequestResponse::from);
    }

    /** Acheteur : détail d'une demande de retour. */
    @Transactional(readOnly = true)
    public ReturnRequestResponse getMine(User buyer, UUID id) {
        ReturnRequest r = getWithDetailsOrThrow(id);
        if (!r.getBuyer().getId().equals(buyer.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return ReturnRequestResponse.from(r);
    }

    // ── Vendeur ───────────────────────────────────────────────────────────────

    /** Vendeur : liste les retours de sa boutique. */
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listForMyShop(User seller, Pageable pageable) {
        var shop = shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Vous n'avez pas de boutique active"));
        return returnRepository
                .findByShopIdWithDetails(shop.getId(), pageable)
                .map(ReturnRequestResponse::from);
    }

    // ── Vendeur / Admin : décision ────────────────────────────────────────────

    /**
     * Décision sur un retour (vendeur ou admin).
     *
     * APPROVED :
     *   - RefundStatus → PROCESSING
     *   - SubOrder     → RETURNED
     *   - Stock restauré pour chaque article (si variante toujours existante)
     *
     * REJECTED :
     *   - RefundStatus → DENIED
     *   - SubOrder     → DELIVERED (retour à l'état précédent)
     */
    @Transactional
    public ReturnRequestResponse decide(User decider, UUID returnId, DecideReturnRequest request) {
        return decide(decider, returnId, request, null);
    }

    @Transactional
    public ReturnRequestResponse decide(User decider, UUID returnId, DecideReturnRequest request, String ip) {
        ReturnRequest returnRequest = getWithDetailsOrThrow(returnId);

        // Ne pas décider deux fois
        if (returnRequest.getDecision() != null) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cette demande a déjà été traitée (décision : " + returnRequest.getDecision() + ")");
        }

        SubOrder subOrder = returnRequest.getSubOrder();

        // Vérification d'ownership pour un vendeur :
        // il ne peut décider que sur les retours de SA boutique.
        // Un admin (role = ADMIN) peut décider sur n'importe quel retour.
        boolean isAdmin = decider.getRole() == com.maplenou.backend.user.Role.ADMIN;
        if (!isAdmin) {
            var shop = shopRepository.findByOwnerId(decider.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                            "Vous n'avez pas de boutique active"));
            if (!subOrder.getShop().getId().equals(shop.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Ce retour ne concerne pas votre boutique");
            }
        }

        if (request.decision() == ReturnDecision.APPROVED) {
            returnRequest.setRefundStatus(RefundStatus.PROCESSING);

            // Restaurer le stock pour chaque article de la sous-commande
            subOrder.getItems().forEach(item -> {
                if (item.getProductVariant() != null) {
                    variantRepository.incrementStock(
                            item.getProductVariant().getId(),
                            item.getQuantity()
                    );
                }
            });

            subOrder.setStatus(SubOrderStatus.RETURNED);

        } else {
            // REJECTED
            returnRequest.setRefundStatus(RefundStatus.DENIED);
            subOrder.setStatus(SubOrderStatus.DELIVERED);
        }

        returnRequest.setDecision(request.decision());
        returnRequest.setDecidedAt(Instant.now());
        returnRequest.setDecidedBy(decider);
        returnRequest.setAdminNotes(request.adminNotes());

        subOrderRepository.save(subOrder);
        returnRepository.save(returnRequest);
        notificationService.returnDecided(
                returnRequest.getBuyer().getId(),
                returnRequest.getId(),
                returnRequest.getDecision().name());

        auditService.log(decider.getId(), AuditAction.RETURN_DECIDED, "RETURN_REQUEST", returnId,
                "Decision: " + request.decision(), ip);

        return reload(returnRequest.getId());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Admin : liste les retours en attente de décision. */
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> listPending(Pageable pageable) {
        return returnRepository
                .findPendingDecision(pageable)
                .map(ReturnRequestResponse::from);
    }

    /** Admin : détail d'un retour. */
    @Transactional(readOnly = true)
    public ReturnRequestResponse getById(UUID id) {
        return ReturnRequestResponse.from(getWithDetailsOrThrow(id));
    }

    // ── Privé ─────────────────────────────────────────────────────────────────

    private ReturnRequest getWithDetailsOrThrow(UUID id) {
        return returnRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Demande de retour introuvable"));
    }

    private ReturnRequestResponse reload(UUID id) {
        return ReturnRequestResponse.from(returnRepository.findByIdWithDetails(id).orElseThrow());
    }
}
