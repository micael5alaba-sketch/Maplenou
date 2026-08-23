package com.maplenou.backend.promo;

import com.maplenou.backend.promo.dto.*;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "Promo Codes", description = "Gestion des codes promotionnels")
@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    // ── Création ──────────────────────────────────────────────────────────────

    @Operation(summary = "Créer un code promo (admin ou vendeur pour sa boutique)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public ResponseEntity<PromoCodeResponse> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePromoCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promoCodeService.create(user, request));
    }

    // ── Activation / Désactivation ────────────────────────────────────────────

    @Operation(summary = "Activer un code promo")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public PromoCodeResponse activate(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return promoCodeService.setActive(user, id, true);
    }

    @Operation(summary = "Désactiver un code promo")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public PromoCodeResponse deactivate(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return promoCodeService.setActive(user, id, false);
    }

    // ── Validation (avant commande) ───────────────────────────────────────────

    @Operation(summary = "Vérifier la validité d'un code promo avant de passer commande")
    @PostMapping("/validate")
    public ValidatePromoResponse validate(
            @AuthenticationPrincipal User buyer,
            @Valid @RequestBody ValidatePromoRequest request) {
        return promoCodeService.validate(buyer, request);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────

    @Operation(summary = "Codes promo de la plateforme (admin)")
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<PromoCodeResponse> listPlatformCodes(
            @PageableDefault(size = 20) Pageable pageable) {
        return promoCodeService.listPlatformCodes(pageable);
    }

    @Operation(summary = "Codes promo d'une boutique (admin ou vendeur propriétaire)")
    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public Page<PromoCodeResponse> listShopCodes(
            @AuthenticationPrincipal User user,
            @PathVariable UUID shopId,
            @PageableDefault(size = 20) Pageable pageable) {
        return promoCodeService.listShopCodes(user, shopId, pageable);
    }

    @Operation(summary = "Détail d'un code promo (admin ou vendeur propriétaire)")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    public PromoCodeResponse getById(@PathVariable UUID id) {
        return promoCodeService.getById(id);
    }
}
