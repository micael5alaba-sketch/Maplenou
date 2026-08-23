package com.maplenou.backend.returns;

import com.maplenou.backend.returns.dto.CreateReturnRequest;
import com.maplenou.backend.returns.dto.DecideReturnRequest;
import com.maplenou.backend.returns.dto.ReturnRequestResponse;
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
@RequestMapping("/api/return-requests")
@RequiredArgsConstructor
@Tag(name = "Demandes de retour")
@SecurityRequirement(name = "bearerAuth")
public class ReturnRequestController {

    private final ReturnRequestService returnService;

    // ── Acheteur ──────────────────────────────────────────────────────────────

    /**
     * Créer une demande de retour.
     * Conditions : sous-commande DELIVERED + dans les 30 jours après livraison.
     */
    @PostMapping
    public ResponseEntity<ReturnRequestResponse> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnService.create(currentUser, request));
    }

    /** Mes demandes de retour (acheteur). */
    @GetMapping("/my")
    public Page<ReturnRequestResponse> listMine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return returnService.listMine(currentUser, pageable);
    }

    /** Détail d'une de mes demandes de retour (acheteur). */
    @GetMapping("/my/{id}")
    public ReturnRequestResponse getMine(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        return returnService.getMine(currentUser, id);
    }

    // ── Vendeur ───────────────────────────────────────────────────────────────

    /** Retours reçus sur ma boutique (vendeur). */
    @GetMapping("/shop")
    @PreAuthorize("hasRole('SELLER')")
    public Page<ReturnRequestResponse> listForMyShop(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return returnService.listForMyShop(currentUser, pageable);
    }

    /**
     * Vendeur ou admin : décider d'un retour (APPROVED / REJECTED).
     * - APPROVED → stock restauré + SubOrder = RETURNED + RemboursementStatus = PROCESSING
     * - REJECTED → SubOrder = DELIVERED + RefundStatus = DENIED
     */
    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ReturnRequestResponse decide(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody DecideReturnRequest request,
            HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        return returnService.decide(currentUser, id, request, ip);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    /** Admin : retours en attente de décision. */
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ReturnRequestResponse> listPending(
            @PageableDefault(size = 20) Pageable pageable) {
        return returnService.listPending(pageable);
    }

    /** Admin : détail d'un retour. */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ReturnRequestResponse getById(@PathVariable UUID id) {
        return returnService.getById(id);
    }
}
