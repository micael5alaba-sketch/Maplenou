package com.maplenou.backend.order;

import com.maplenou.backend.order.dto.SubOrderResponse;
import com.maplenou.backend.order.dto.UpdateSubOrderStatusRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sub-orders")
@RequiredArgsConstructor
@Tag(name = "Sous-commandes (vendeur)")
@SecurityRequirement(name = "bearerAuth")
public class SubOrderController {

    private final SubOrderService subOrderService;

    /** Liste les sous-commandes de ma boutique. */
    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public Page<SubOrderResponse> listMySubOrders(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return subOrderService.listMySubOrders(currentUser, pageable);
    }

    /** Détail d'une sous-commande de ma boutique. */
    @GetMapping("/{subOrderId}")
    @PreAuthorize("hasRole('SELLER')")
    public SubOrderResponse getMySubOrder(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID subOrderId) {
        return subOrderService.getMySubOrder(currentUser, subOrderId);
    }

    /** Vendeur : passe au statut CONFIRMED ou SHIPPED. */
    @PatchMapping("/{subOrderId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public SubOrderResponse updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID subOrderId,
            @Valid @RequestBody UpdateSubOrderStatusRequest request) {
        return subOrderService.updateStatus(currentUser, subOrderId, request.status());
    }

    /** Admin : peut forcer n'importe quel statut. */
    @PatchMapping("/admin/{subOrderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SubOrderResponse adminUpdateStatus(
            @PathVariable UUID subOrderId,
            @Valid @RequestBody UpdateSubOrderStatusRequest request,
            @AuthenticationPrincipal User currentUser,
            HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        return subOrderService.adminUpdateStatus(subOrderId, request.status(), currentUser.getId(), ip);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
