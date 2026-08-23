package com.maplenou.backend.order;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.order.dto.SubOrderResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubOrderService {

    private final SubOrderRepository subOrderRepository;
    private final ShopRepository shopRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ----- Lecture vendeur -----

    @Transactional(readOnly = true)
    public Page<SubOrderResponse> listMySubOrders(User seller, Pageable pageable) {
        Shop shop = getShopOrThrow(seller);
        return subOrderRepository
                .findByShopIdOrderByCreatedAtDesc(shop.getId(), pageable)
                .map(SubOrderResponse::from);
    }

    @Transactional(readOnly = true)
    public SubOrderResponse getMySubOrder(User seller, UUID subOrderId) {
        Shop shop = getShopOrThrow(seller);
        SubOrder so = subOrderRepository.findByIdWithDetails(subOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));
        if (!so.getShop().getId().equals(shop.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        return SubOrderResponse.from(so);
    }

    // ----- Mise à jour du statut (vendeur) -----
    // Transitions autorisées au vendeur :
    //   PENDING → PREPARING
    //   PREPARING → READY_FOR_PICKUP

    @Transactional
    public SubOrderResponse updateStatus(User seller, UUID subOrderId, SubOrderStatus newStatus) {
        Shop shop = getShopOrThrow(seller);
        SubOrder so = subOrderRepository.findByIdAndShopId(subOrderId, shop.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        validateSellerTransition(so.getStatus(), newStatus);
        so.setStatus(newStatus);
        subOrderRepository.save(so);
        SubOrder updated = subOrderRepository.findByIdWithDetails(so.getId()).orElseThrow();
        notificationService.subOrderStatusChanged(
                updated.getOrder().getBuyer().getId(),
                updated.getId(),
                updated.getStatus().name(),
                updated.getShop().getName());
        return SubOrderResponse.from(updated);
    }

    // ----- Mise à jour du statut (admin) -----

    @Transactional
    public SubOrderResponse adminUpdateStatus(UUID subOrderId, SubOrderStatus newStatus) {
        return adminUpdateStatus(subOrderId, newStatus, null, null);
    }

    @Transactional
    public SubOrderResponse adminUpdateStatus(UUID subOrderId, SubOrderStatus newStatus,
                                               UUID actorId, String ip) {
        SubOrder so = subOrderRepository.findByIdWithDetails(subOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        if (isTerminalStatus(so.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cette sous-commande est dans un état final (" + so.getStatus() + ")");
        }

        so.setStatus(newStatus);
        subOrderRepository.save(so);
        SubOrder updatedByAdmin = subOrderRepository.findByIdWithDetails(so.getId()).orElseThrow();
        notificationService.subOrderStatusChanged(
                updatedByAdmin.getOrder().getBuyer().getId(),
                updatedByAdmin.getId(),
                updatedByAdmin.getStatus().name(),
                updatedByAdmin.getShop().getName());

        auditService.log(actorId, AuditAction.SUBORDER_STATUS_CHANGED_ADMIN, "SUB_ORDER", subOrderId,
                "Status changed to " + newStatus, ip);

        return SubOrderResponse.from(updatedByAdmin);
    }

    // ----- Privé -----

    private void validateSellerTransition(SubOrderStatus current, SubOrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == SubOrderStatus.PREPARING;
            case PREPARING  -> next == SubOrderStatus.READY_FOR_PICKUP;
            default         -> false;
        };
        if (!valid) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Transition " + current + " → " + next + " non autorisée. "
                    + "Vous pouvez : PENDING→PREPARING, PREPARING→READY_FOR_PICKUP");
        }
    }

    private boolean isTerminalStatus(SubOrderStatus status) {
        return status == SubOrderStatus.DELIVERED
                || status == SubOrderStatus.RETURNED
                || status == SubOrderStatus.CANCELLED;
    }

    private Shop getShopOrThrow(User seller) {
        return shopRepository.findByOwnerId(seller.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "Vous n'avez pas de boutique active"));
    }
}
