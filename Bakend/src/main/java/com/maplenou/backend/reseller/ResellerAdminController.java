package com.maplenou.backend.reseller;

import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.reseller.dto.ResellerDtos.*;
import com.maplenou.backend.user.User;
import com.maplenou.backend.withdrawal.WithdrawalAction;
import com.maplenou.backend.withdrawal.dto.WithdrawalActionRequest;
import com.maplenou.backend.withdrawal.dto.WithdrawalHistoryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Back-office du programme partenaire : demandes, partenaires, paramètres, commissions, retraits. */
@RestController
@RequestMapping("/api/admin/resellers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Programme partenaire revendeur")
public class ResellerAdminController {

    private final ResellerService resellerService;
    private final ResellerSettingsService settingsService;
    private final ResellerLinkService linkService;
    private final ResellerAttributionService attributionService;
    private final ResellerWalletService walletService;
    private final ResellerWithdrawalService withdrawalService;

    // ----- Partenaires -----

    @GetMapping
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public Page<ProfileResponse> list(@RequestParam(required = false) ResellerStatus status,
                                      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return resellerService.list(status, pageable);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public AdminStats stats() {
        return resellerService.stats();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public ProfileResponse get(@PathVariable UUID id) {
        return resellerService.get(id);
    }

    @GetMapping("/{id}/wallet")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public WalletResponse wallet(@PathVariable UUID id) {
        return walletService.wallet(id);
    }

    @GetMapping("/{id}/wallet/transactions")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public Page<WalletTransactionResponse> transactions(@PathVariable UUID id,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return walletService.transactions(id, pageable);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public ProfileResponse approve(@PathVariable UUID id, @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return resellerService.approve(id, actor, req.getRemoteAddr());
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public ProfileResponse reject(@PathVariable UUID id, @RequestBody ReasonRequest body,
                                  @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return resellerService.reject(id, body.reason(), actor, req.getRemoteAddr());
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public ProfileResponse suspend(@PathVariable UUID id, @RequestBody ReasonRequest body,
                                   @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return resellerService.suspend(id, body.reason(), actor, req.getRemoteAddr());
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public ProfileResponse reactivate(@PathVariable UUID id, @RequestBody ReasonRequest body,
                                      @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return resellerService.reactivate(id, body.reason(), actor, req.getRemoteAddr());
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public ProfileResponse deactivate(@PathVariable UUID id, @RequestBody ReasonRequest body,
                                      @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return resellerService.deactivate(id, body.reason(), actor, req.getRemoteAddr());
    }

    /** Régularisation du portefeuille (Super Admin : droit de paramétrage financier). */
    @PostMapping("/{id}/adjustments")
    @PreAuthorize("hasAuthority('RESELLERS_SETTINGS')")
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse adjust(@PathVariable UUID id, @Valid @RequestBody AdjustmentRequest body,
                                 @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return walletService.adjust(id, body, actor, req.getRemoteAddr());
    }

    // ----- Paramètres -----

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public SettingsResponse settings() {
        return settingsService.getCurrent();
    }

    @GetMapping("/settings/history")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public Page<SettingsResponse> settingsHistory(@PageableDefault(size = 20) Pageable pageable) {
        return settingsService.history(pageable);
    }

    @PutMapping("/settings")
    @PreAuthorize("hasAuthority('RESELLERS_SETTINGS')")
    public SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest body,
                                           @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return settingsService.update(body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/products/{productId}/exclusion")
    @PreAuthorize("hasAuthority('RESELLERS_WRITE')")
    public void exclusion(@PathVariable UUID productId, @RequestParam boolean excluded,
                          @RequestBody ReasonRequest body, @AuthenticationPrincipal User actor, HttpServletRequest req) {
        linkService.setProductExcluded(productId, excluded, body.reason(), actor, req.getRemoteAddr());
    }

    // ----- Commissions et journal -----

    @GetMapping("/commissions")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public Page<CommissionResponse> commissions(@PageableDefault(size = 20) Pageable pageable) {
        return attributionService.allCommissions(pageable);
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('RESELLERS_READ')")
    public Page<AuditEntry> audit(@PageableDefault(size = 20) Pageable pageable) {
        return resellerService.audit(pageable);
    }

    // ----- Retraits (même circuit que les autres retraits) -----

    @GetMapping("/withdrawals")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public Page<WithdrawalResponse> withdrawals(@RequestParam(required = false) PayoutStatus status,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return withdrawalService.listAll(status, pageable);
    }

    @GetMapping("/withdrawals/{id}")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public WithdrawalResponse withdrawal(@PathVariable UUID id) {
        return withdrawalService.get(id);
    }

    @GetMapping("/withdrawals/{id}/history")
    @PreAuthorize("hasAuthority('PAYOUTS_READ')")
    public List<WithdrawalHistoryResponse> withdrawalHistory(@PathVariable UUID id) {
        return withdrawalService.history(id);
    }

    @PatchMapping("/withdrawals/{id}/prepare")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public WithdrawalResponse prepare(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                      @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.PREPARE, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/return")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public WithdrawalResponse returnForCorrection(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                                  @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.RETURN_FOR_CORRECTION, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/submit")
    @PreAuthorize("hasAuthority('PAYOUTS_PREPARE')")
    public WithdrawalResponse submit(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                     @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.SUBMIT, body, actor, req.getRemoteAddr());
    }

    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public WithdrawalResponse approveWithdrawal(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                                @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.APPROVE, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/reject")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public WithdrawalResponse rejectWithdrawal(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                               @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.REJECT, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/processing")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public WithdrawalResponse processing(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                         @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.PROCESS, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/paid")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public WithdrawalResponse paid(@PathVariable UUID id, @RequestBody(required = false) WithdrawalActionRequest body,
                                   @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.PAY, body, actor, req.getRemoteAddr());
    }

    @PatchMapping("/withdrawals/{id}/failed")
    @PreAuthorize("hasAuthority('PAYOUTS_APPROVE')")
    public WithdrawalResponse failed(@PathVariable UUID id, @RequestBody WithdrawalActionRequest body,
                                     @AuthenticationPrincipal User actor, HttpServletRequest req) {
        return withdrawalService.transition(id, WithdrawalAction.FAIL, body, actor, req.getRemoteAddr());
    }
}
