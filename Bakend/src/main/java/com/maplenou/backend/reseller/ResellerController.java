package com.maplenou.backend.reseller;

import com.maplenou.backend.reseller.dto.ResellerDtos.*;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** API du partenaire revendeur (compte connecté) + routes publiques de lien et de clic. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Programme partenaire revendeur")
public class ResellerController {

    private final ResellerService resellerService;
    private final ResellerLinkService linkService;
    private final ResellerAttributionService attributionService;
    private final ResellerWalletService walletService;
    private final ResellerWithdrawalService withdrawalService;
    private final ResellerSettingsService settingsService;

    // ----- Public -----

    /** Lien partenaire partagé : redirige vers l'application web (le clic est enregistré par l'appli). */
    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(linkService.redirectTarget(code)).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    /** Enregistre un clic (visiteur connecté ou non) et renvoie le jeton d'attribution signé. */
    @PostMapping("/api/reseller/clicks")
    public ClickResponse click(@Valid @RequestBody ClickRequest body, @AuthenticationPrincipal User viewer,
                               HttpServletRequest request) {
        return linkService.recordClick(body.code(), body.visitorId(), request.getRemoteAddr(),
                request.getHeader("User-Agent"), viewer);
    }

    /** Conditions financières en vigueur (taux, seuils) — affichées dans l'appli. */
    @GetMapping("/api/reseller/settings")
    public SettingsResponse settings() {
        return settingsService.getCurrent();
    }

    // ----- Partenaire connecté -----

    @PostMapping("/api/reseller/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse apply(@AuthenticationPrincipal User user, @Valid @RequestBody ApplyRequest body,
                                 HttpServletRequest request) {
        return resellerService.apply(user, request.getRemoteAddr());
    }

    @GetMapping("/api/reseller/me")
    public ProfileResponse me(@AuthenticationPrincipal User user) {
        return resellerService.me(user);
    }

    @GetMapping("/api/reseller/catalog")
    public Page<CatalogItem> catalog(@AuthenticationPrincipal User user, @PageableDefault(size = 20) Pageable pageable) {
        return linkService.catalog(user, pageable);
    }

    @PostMapping("/api/reseller/links")
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse createLink(@AuthenticationPrincipal User user, @Valid @RequestBody CreateLinkRequest body) {
        return linkService.createLink(user, body.productId());
    }

    @GetMapping("/api/reseller/links")
    public Page<LinkResponse> links(@AuthenticationPrincipal User user, @PageableDefault(size = 20) Pageable pageable) {
        return linkService.myLinks(user, pageable);
    }

    @GetMapping("/api/reseller/commissions")
    public Page<CommissionResponse> commissions(@AuthenticationPrincipal User user,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return attributionService.myCommissions(resellerService.requireProfile(user).getId(), pageable);
    }

    @GetMapping("/api/reseller/wallet")
    public WalletResponse wallet(@AuthenticationPrincipal User user) {
        return walletService.wallet(resellerService.requireProfile(user).getId());
    }

    @GetMapping("/api/reseller/wallet/transactions")
    public Page<WalletTransactionResponse> transactions(@AuthenticationPrincipal User user,
                                                        @PageableDefault(size = 20) Pageable pageable) {
        return walletService.transactions(resellerService.requireProfile(user).getId(), pageable);
    }

    @PostMapping("/api/reseller/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public WithdrawalResponse withdraw(@AuthenticationPrincipal User user, @Valid @RequestBody WithdrawRequest body,
                                       @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return withdrawalService.request(user, body, idempotencyKey);
    }

    @GetMapping("/api/reseller/withdrawals")
    public Page<WithdrawalResponse> myWithdrawals(@AuthenticationPrincipal User user,
                                                  @PageableDefault(size = 20) Pageable pageable) {
        return withdrawalService.listMine(user, pageable);
    }
}
