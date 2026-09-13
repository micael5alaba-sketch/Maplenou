package com.maplenou.backend.payout;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payout.dto.CreatePayoutMethodRequest;
import com.maplenou.backend.payout.dto.PayoutMethodResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Moyens de paiement enregistrés par un vendeur pour recevoir ses versements
 * (T-Money, Flooz, virement bancaire). Purement déclaratif : le virement
 * réel reste initié manuellement par l'admin via {@link PayoutController}.
 */
@RestController
@RequestMapping("/api/seller/payout-methods")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Moyens de paiement vendeur")
public class PayoutMethodController {

    private final PayoutMethodService payoutMethodService;
    private final ShopRepository shopRepository;

    @GetMapping
    public List<PayoutMethodResponse> listMine(@AuthenticationPrincipal User currentUser) {
        return payoutMethodService.listForShop(getShop(currentUser).getId());
    }

    @PostMapping
    public ResponseEntity<PayoutMethodResponse> create(@AuthenticationPrincipal User currentUser,
                                                         @Valid @RequestBody CreatePayoutMethodRequest request) {
        PayoutMethodResponse created = payoutMethodService.create(getShop(currentUser), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/default")
    public PayoutMethodResponse setDefault(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        return payoutMethodService.setDefault(getShop(currentUser), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        payoutMethodService.delete(getShop(currentUser), id);
    }

    private Shop getShop(User currentUser) {
        return shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucune boutique associée à ce compte"));
    }
}
