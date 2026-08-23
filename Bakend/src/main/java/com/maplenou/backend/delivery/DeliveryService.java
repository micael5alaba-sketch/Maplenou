package com.maplenou.backend.delivery;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.AssignAgentRequest;
import com.maplenou.backend.delivery.dto.DeliveryResponse;
import com.maplenou.backend.delivery.dto.NavigationResponse;
import com.maplenou.backend.delivery.dto.UpdateDeliveryRequest;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final SubOrderRepository subOrderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ----- Admin -----

    /**
     * Crée une Delivery pour un SubOrder passé en READY_FOR_PICKUP.
     * Appelé par l'admin quand il voit qu'un colis est prêt à être récupéré.
     */
    @Transactional
    public DeliveryResponse createForSubOrder(UUID subOrderId) {
        SubOrder subOrder = subOrderRepository.findById(subOrderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sous-commande introuvable"));

        if (subOrder.getStatus() != SubOrderStatus.READY_FOR_PICKUP) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La livraison ne peut être créée que pour un sous-ordre au statut READY_FOR_PICKUP");
        }

        if (deliveryRepository.findBySubOrderId(subOrderId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Une livraison existe déjà pour cette sous-commande");
        }

        Delivery delivery = Delivery.builder()
                .subOrder(subOrder)
                .status(DeliveryStatus.PENDING)
                .build();

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }

    /** Admin : affecter un livreur à une livraison en attente. */
    @Transactional
    public DeliveryResponse assignAgent(UUID deliveryId, AssignAgentRequest request) {
        Delivery delivery = getDeliveryOrThrow(deliveryId);

        User agent = userRepository.findById(request.agentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if (agent.getRole() != Role.DELIVERY_AGENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Cet utilisateur n'est pas un livreur (DELIVERY_AGENT)");
        }

        if (delivery.getStatus() != DeliveryStatus.PENDING && delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Impossible d'affecter un livreur à une livraison au statut " + delivery.getStatus());
        }

        delivery.setAgent(agent);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(Instant.now());

        deliveryRepository.save(delivery);
        notificationService.deliveryAssigned(
                agent.getId(), delivery.getId(), delivery.getSubOrder().getShop().getName());
        return reload(delivery.getId());
    }

    /** Admin : livraisons en attente d'affectation. */
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> listPending(Pageable pageable) {
        return deliveryRepository.findByStatusOrderByCreatedAtAsc(DeliveryStatus.PENDING, pageable)
                .map(DeliveryResponse::from);
    }

    /** Admin : détail d'une livraison. */
    @Transactional(readOnly = true)
    public DeliveryResponse getById(UUID id) {
        return DeliveryResponse.from(getDeliveryWithDetailsOrThrow(id));
    }

    // ----- Livreur -----

    /** Livreur : mes livraisons assignées. */
    @Transactional(readOnly = true)
    public Page<DeliveryResponse> listMyDeliveries(User agent, Pageable pageable) {
        return deliveryRepository.findByAgentIdOrderByCreatedAtDesc(agent.getId(), pageable)
                .map(DeliveryResponse::from);
    }

    /**
     * Livreur : données de navigation Google Maps pour une livraison.
     * Fournit le point de retrait (boutique) et le point de livraison (adresse client).
     */
    @Transactional(readOnly = true)
    public NavigationResponse getNavigation(User agent, UUID deliveryId) {
        Delivery delivery = getDeliveryWithDetailsOrThrow(deliveryId);

        if (delivery.getAgent() == null || !delivery.getAgent().getId().equals(agent.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette livraison ne vous est pas assignée");
        }

        SubOrder subOrder = delivery.getSubOrder();
        var shop = subOrder.getShop();
        var addrSnapshot = subOrder.getOrder().getDeliveryAddress();

        NavigationResponse.PickupPoint pickup = new NavigationResponse.PickupPoint(
                shop.getId(),
                shop.getName(),
                shop.getCity() + (shop.getDistrict() != null ? ", " + shop.getDistrict() : ""),
                shop.getLatitude(),
                shop.getLongitude()
        );

        NavigationResponse.DropoffPoint dropoff = new NavigationResponse.DropoffPoint(
                subOrder.getOrder().getBuyer().getFullName(),
                addrSnapshot.getCity()
                        + (addrSnapshot.getDistrict() != null ? ", " + addrSnapshot.getDistrict() : ""),
                addrSnapshot.getLatitude(),
                addrSnapshot.getLongitude()
        );

        return new NavigationResponse(deliveryId, pickup, dropoff);
    }

    /**
     * Livreur : mettre à jour le statut.
     * ASSIGNED → IN_TRANSIT → DELIVERED (proofType + proofUrl obligatoires) | FAILED
     */
    @Transactional
    public DeliveryResponse updateStatus(User agent, UUID deliveryId, UpdateDeliveryRequest request) {
        Delivery delivery = getDeliveryWithDetailsOrThrow(deliveryId);

        if (delivery.getAgent() == null || !delivery.getAgent().getId().equals(agent.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette livraison ne vous est pas assignée");
        }

        validateAgentTransition(delivery.getStatus(), request.status());

        SubOrder subOrder = delivery.getSubOrder();

        if (request.status() == DeliveryStatus.IN_TRANSIT) {
            // Synchroniser le sous-ordre → IN_DELIVERY
            subOrder.setStatus(SubOrderStatus.IN_DELIVERY);
            subOrderRepository.save(subOrder);
            notificationService.subOrderStatusChanged(
                    subOrder.getOrder().getBuyer().getId(),
                    subOrder.getId(),
                    SubOrderStatus.IN_DELIVERY.name(),
                    subOrder.getShop().getName());
        }

        if (request.status() == DeliveryStatus.DELIVERED) {
            if (request.proofUrl() == null || request.proofUrl().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "La preuve de livraison (proofUrl) est obligatoire");
            }
            if (request.proofType() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Le type de preuve (PHOTO ou SIGNATURE) est obligatoire");
            }
            delivery.setProofUrl(request.proofUrl());
            delivery.setProofType(request.proofType());
            delivery.setDeliveredAt(Instant.now());

            // Synchroniser le sous-ordre → DELIVERED
            subOrder.setStatus(SubOrderStatus.DELIVERED);
            subOrderRepository.save(subOrder);
            notificationService.subOrderStatusChanged(
                    subOrder.getOrder().getBuyer().getId(),
                    subOrder.getId(),
                    SubOrderStatus.DELIVERED.name(),
                    subOrder.getShop().getName());
        }

        delivery.setStatus(request.status());
        if (request.notes() != null) delivery.setNotes(request.notes());

        deliveryRepository.save(delivery);
        return reload(delivery.getId());
    }

    // ----- Privé -----

    private void validateAgentTransition(DeliveryStatus current, DeliveryStatus next) {
        boolean valid = switch (current) {
            case ASSIGNED   -> next == DeliveryStatus.IN_TRANSIT;
            case IN_TRANSIT -> next == DeliveryStatus.DELIVERED || next == DeliveryStatus.FAILED;
            default         -> false;
        };
        if (!valid) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Transition " + current + " → " + next + " non autorisée");
        }
    }

    private Delivery getDeliveryOrThrow(UUID id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
    }

    private Delivery getDeliveryWithDetailsOrThrow(UUID id) {
        return deliveryRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Livraison introuvable"));
    }

    private DeliveryResponse reload(UUID id) {
        return DeliveryResponse.from(deliveryRepository.findByIdWithDetails(id).orElseThrow());
    }
}
