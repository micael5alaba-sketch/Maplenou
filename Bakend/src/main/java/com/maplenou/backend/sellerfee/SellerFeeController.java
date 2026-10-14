package com.maplenou.backend.sellerfee;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.sellerfee.dto.RequestCategoryChangeRequest;
import com.maplenou.backend.sellerfee.dto.SellerFeeCategoryChangeResponse;
import com.maplenou.backend.sellerfee.dto.SellerFeeDeductionResponse;
import com.maplenou.backend.sellerfee.dto.SellerFeeSummaryResponse;
import com.maplenou.backend.sellerfee.dto.SellerMonthlyFeeResponse;
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

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Seller Fees - Frais mensuels d'activité vendeur")
public class SellerFeeController {

    private final SellerFeeService sellerFeeService;
    private final ShopRepository shopRepository;

    /** GET /api/seller/fees/summary — catégorie active, frais impayés, solde, net estimé. */
    @GetMapping("/api/seller/fees/summary")
    @PreAuthorize("hasRole('SELLER')")
    public SellerFeeSummaryResponse getSummary(@AuthenticationPrincipal User currentUser) {
        Shop shop = myShop(currentUser);
        return sellerFeeService.getSummary(currentUser, shop.getId());
    }

    /** GET /api/seller/fees/history — un frais mensuel par ligne (dû/payé/annulé). */
    @GetMapping("/api/seller/fees/history")
    @PreAuthorize("hasRole('SELLER')")
    public Page<SellerMonthlyFeeResponse> getFeeHistory(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Shop shop = myShop(currentUser);
        return sellerFeeService.getFeeHistory(shop.getId(), pageable).map(SellerMonthlyFeeResponse::from);
    }

    /** GET /api/seller/fees/deductions — historique des déductions réelles, avec référence de reversement. */
    @GetMapping("/api/seller/fees/deductions")
    @PreAuthorize("hasRole('SELLER')")
    public Page<SellerFeeDeductionResponse> getDeductionHistory(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Shop shop = myShop(currentUser);
        return sellerFeeService.getDeductionHistory(shop.getId(), pageable).map(SellerFeeDeductionResponse::from);
    }

    /** POST /api/seller/fees/category-change — demande de changement de catégorie tarifaire. */
    @PostMapping("/api/seller/fees/category-change")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerFeeCategoryChangeResponse> requestCategoryChange(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RequestCategoryChangeRequest request) {
        var saved = sellerFeeService.requestCategoryChange(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SellerFeeCategoryChangeResponse.from(saved));
    }

    private Shop myShop(User currentUser) {
        return shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucune boutique associée à ce compte"));
    }
}
