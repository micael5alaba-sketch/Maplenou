package com.maplenou.backend.delivery;

import com.maplenou.backend.delivery.dto.AssignAgentRequest;
import com.maplenou.backend.delivery.dto.DeliveryResponse;
import com.maplenou.backend.delivery.dto.NavigationResponse;
import com.maplenou.backend.delivery.dto.UpdateDeliveryRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Livraisons")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ----- Admin -----

    /** Admin : créer la livraison pour un SubOrder passé en READY_FOR_PICKUP. */
    @PostMapping("/sub-orders/{subOrderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DeliveryResponse> createForSubOrder(@PathVariable UUID subOrderId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryService.createForSubOrder(subOrderId));
    }

    /** Admin : affecter un livreur. */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public DeliveryResponse assignAgent(@PathVariable UUID id,
                                        @Valid @RequestBody AssignAgentRequest request) {
        return deliveryService.assignAgent(id, request);
    }

    /** Admin : livraisons en attente d'affectation. */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<DeliveryResponse> listPending(@PageableDefault(size = 20) Pageable pageable) {
        return deliveryService.listPending(pageable);
    }

    /** Admin : détail d'une livraison. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeliveryResponse getById(@PathVariable UUID id) {
        return deliveryService.getById(id);
    }

    // ----- Livreur -----

    /** Livreur : mes livraisons assignées. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public Page<DeliveryResponse> listMyDeliveries(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return deliveryService.listMyDeliveries(currentUser, pageable);
    }

    /**
     * Livreur : navigation Google Maps.
     * Retourne les coordonnées du point de retrait (boutique) et du point de livraison (client).
     * Flutter utilise ces données pour ouvrir l'itinéraire dans Google Maps.
     */
    @GetMapping("/{id}/navigation")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public NavigationResponse getNavigation(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return deliveryService.getNavigation(currentUser, id);
    }

    /**
     * Livreur : mettre à jour le statut de sa livraison.
     * ASSIGNED → IN_TRANSIT → DELIVERED (proofType + proofUrl) | FAILED
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public DeliveryResponse updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryRequest request) {
        return deliveryService.updateStatus(currentUser, id, request);
    }
}
