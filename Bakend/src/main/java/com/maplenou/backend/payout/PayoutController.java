package com.maplenou.backend.payout;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payout.dto.GeneratePayoutsRequest;
import com.maplenou.backend.payout.dto.GeneratePayoutsResponse;
import com.maplenou.backend.payout.dto.PayoutResponse;
import com.maplenou.backend.withdrawal.WithdrawalAction;
import com.maplenou.backend.withdrawal.dto.WithdrawalActionRequest;
import com.maplenou.backend.withdrawal.dto.WithdrawalHistoryResponse;
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

import java.util.List;
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

    /**
     * POST /api/seller/payouts/withdraw
     * Vendeur : retrait à la demande de son solde disponible — pas d'attente du cron
     * hebdomadaire, pas de validation admin préalable (décision produit). L'admin garde
     * la main sur l'exécution réelle du virement (PayoutController admin, markAsPaid).
     */
    @PostMapping("/api/seller/payouts/withdraw")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<PayoutResponse> requestWithdrawal(@AuthenticationPrincipal User currentUser) {
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Aucune boutique associée à ce compte"));
        return ResponseEntity.status(HttpStatus.CREATED).body(payoutService.requestWithdrawal(shop));
    }

    /**
     * GET /api/seller/payouts/{id}
     * Vendeur : détail d'un de ses reversements, avec le détail des sous-commandes incluses
     * (pour générer une facture téléchargeable côté app).
     */
    @GetMapping("/api/seller/payouts/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public com.maplenou.backend.payout.dto.PayoutDetailResponse getMyPayoutDetail(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id) {
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Aucune boutique associée à ce compte"));
        return payoutService.getSellerPayoutDetail(id, shop.getId());
    }

    // ─── Admin ─────────────────────────────────────────────────────────────────

    /** Admin : tous les retraits vendeurs, filtrables par statut (Super Admin, Agent support, Auditeur). */
    @GetMapping("/api/admin/payouts")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public Page<PayoutResponse> listAll(
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return payoutService.listAll(status, pageable);
    }

    @GetMapping("/api/admin/payouts/{id}")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public PayoutResponse getById(@PathVariable UUID id) {
        return payoutService.getById(id);
    }

    /** Historique des transitions de statut (qui, quand, pourquoi). */
    @GetMapping("/api/admin/payouts/{id}/history")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public List<WithdrawalHistoryResponse> history(@PathVariable UUID id) {
        return payoutService.history(id);
    }

    /**
     * Génération manuelle pour une période donnée (test / rattrapage). Les retraits générés
     * entrent dans le circuit au statut PENDING (à vérifier par l'Agent support).
     */
    @PostMapping("/api/admin/payouts/generate")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public ResponseEntity<GeneratePayoutsResponse> generateManual(
            @Valid @RequestBody GeneratePayoutsRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {

        int count = payoutService.generateManual(request, actor.getId(), httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new GeneratePayoutsResponse(count, request.periodStart(), request.periodEnd()));
    }

    // ----- Préparation (Agent support et retraits) : jamais d'approbation ni de paiement -----

    /** Vérification terminée : le dossier est conforme. */
    @PatchMapping("/api/admin/payouts/{id}/prepare")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public PayoutResponse prepare(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                  @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.PREPARE, body, actor, request.getRemoteAddr());
    }

    /** Renvoi pour correction (motif obligatoire). */
    @PatchMapping("/api/admin/payouts/{id}/return")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public PayoutResponse returnForCorrection(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                              @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.RETURN_FOR_CORRECTION, body, actor, request.getRemoteAddr());
    }

    /** Soumission du dossier préparé au Super Admin. */
    @PatchMapping("/api/admin/payouts/{id}/submit")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public PayoutResponse submit(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                 @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.SUBMIT, body, actor, request.getRemoteAddr());
    }

    // ----- Décision et exécution (Super Admin uniquement) -----

    /** Approbation finale : nouveau code MFA exigé dans le corps ({@code mfaCode}). */
    @PostMapping("/api/admin/payouts/{id}/approve")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public PayoutResponse approve(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                  @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.APPROVE, body, actor, request.getRemoteAddr());
    }

    /** Rejet (justification obligatoire) : les sous-commandes et frais sont libérés. */
    @PatchMapping("/api/admin/payouts/{id}/reject")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public PayoutResponse reject(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                 @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.REJECT, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/payouts/{id}/processing")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public PayoutResponse markProcessing(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                         @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.PROCESS, body, actor, request.getRemoteAddr());
    }

    /** Virement effectué : uniquement pour un retrait approuvé. */
    @PatchMapping("/api/admin/payouts/{id}/paid")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public PayoutResponse markPaid(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                   @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.PAY, body, actor, request.getRemoteAddr());
    }

    @PatchMapping("/api/admin/payouts/{id}/failed")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public PayoutResponse markFailed(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                     @AuthenticationPrincipal User actor, HttpServletRequest request) {
        return payoutService.transition(id, WithdrawalAction.FAIL, body, actor, request.getRemoteAddr());
    }
}
