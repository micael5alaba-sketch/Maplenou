package com.maplenou.backend.delivery;

import com.maplenou.backend.delivery.dto.CourierBalanceResponse;
import com.maplenou.backend.delivery.dto.CourierEarningResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Rémunération livreur")
@SecurityRequirement(name = "bearerAuth")
public class CourierEarningController {

    private final CourierEarningService courierEarningService;

    // ----- Livreur -----

    /** Historique de mes gains, un par livraison terminée. */
    @GetMapping("/api/delivery-agents/me/earnings")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public Page<CourierEarningResponse> listMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return courierEarningService.listMine(currentUser, pageable);
    }

    /** Solde disponible : somme des gains pas encore versés (PENDING). */
    @GetMapping("/api/delivery-agents/me/earnings/balance")
    @PreAuthorize("hasRole('DELIVERY_AGENT')")
    public CourierBalanceResponse getBalance(@AuthenticationPrincipal User currentUser) {
        return new CourierBalanceResponse(courierEarningService.getAvailableBalance(currentUser));
    }

    // ----- Admin -----

    /** Admin : tous les gains livreurs, filtrables par statut. */
    @GetMapping("/api/admin/courier-earnings")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CourierEarningResponse> listAllForAdmin(
            @RequestParam(required = false) CourierEarningStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return courierEarningService.listAllForAdmin(status, pageable);
    }

    /** Admin : confirmer qu'un gain a été versé au livreur. */
    @PatchMapping("/api/admin/courier-earnings/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public CourierEarningResponse markAsPaid(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest request) {
        return courierEarningService.markAsPaid(id, actor.getId(), request.getRemoteAddr());
    }
}
