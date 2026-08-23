package com.maplenou.backend.payout;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payout.dto.GeneratePayoutsRequest;
import com.maplenou.backend.payout.dto.GeneratePayoutsResponse;
import com.maplenou.backend.payout.dto.PayoutResponse;
import com.maplenou.backend.payout.dto.UpdatePayoutStatusRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payouts - Reversements vendeurs")
public class PayoutController {

    private final PayoutService payoutService;
    private final ShopRepository shopRepository;

    // ─── Vendeur ───────────────────────────────────────────────────────────────

    /**
     * GET /api/seller/payouts
     * Vendeur : liste de ses propres reversements.
     */
    @GetMapping("/api/seller/payouts")
    @PreAuthorize("hasRole('SELLER')")
    public Page<PayoutResponse> listMyPayouts(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {

        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Aucune boutique associée à ce compte"));
        return payoutService.listForShop(shop.getId(), pageable);
    }

    // ─── Admin ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/payouts
     * Admin : tous les payouts, filtrables par statut.
     */
    @GetMapping("/api/admin/payouts")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PayoutResponse> listAll(
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return payoutService.listAll(status, pageable);
    }

    /**
     * GET /api/admin/payouts/{id}
     * Admin : détail d'un payout.
     */
    @GetMapping("/api/admin/payouts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PayoutResponse getById(@PathVariable UUID id) {
        return payoutService.getById(id);
    }

    /**
     * POST /api/admin/payouts/generate
     * Admin : génération manuelle pour une période donnée (test / rattrapage).
     * Body: { "periodStart": "...", "periodEnd": "..." }
     */
    @PostMapping("/api/admin/payouts/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GeneratePayoutsResponse> generateManual(
            @Valid @RequestBody GeneratePayoutsRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {

        int count = payoutService.generateManual(request, actor.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GeneratePayoutsResponse(count, request.periodStart(), request.periodEnd()));
    }

    /**
     * PATCH /api/admin/payouts/{id}/processing
     * Admin : marquer un payout en cours de traitement (PROCESSING).
     */
    @PatchMapping("/api/admin/payouts/{id}/processing")
    @PreAuthorize("hasRole('ADMIN')")
    public PayoutResponse markProcessing(
            @PathVariable UUID id,
            @AuthenticationPrincipal User actor,
            HttpServletRequest request) {
        return payoutService.markAsProcessing(id, actor.getId(), request.getRemoteAddr());
    }

    /**
     * PATCH /api/admin/payouts/{id}/paid
     * Admin : confirmer qu'un virement a été effectué.
     */
    @PatchMapping("/api/admin/payouts/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public PayoutResponse markPaid(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePayoutStatusRequest body,
            @AuthenticationPrincipal User actor,
            HttpServletRequest request) {
        return payoutService.markAsPaid(id, body, actor.getId(), request.getRemoteAddr());
    }

    /**
     * PATCH /api/admin/payouts/{id}/failed
     * Admin : marquer un payout comme échoué (à retraiter).
     */
    @PatchMapping("/api/admin/payouts/{id}/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public PayoutResponse markFailed(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePayoutStatusRequest body,
            @AuthenticationPrincipal User actor,
            HttpServletRequest request) {
        return payoutService.markAsFailed(id, body, actor.getId(), request.getRemoteAddr());
    }
}
