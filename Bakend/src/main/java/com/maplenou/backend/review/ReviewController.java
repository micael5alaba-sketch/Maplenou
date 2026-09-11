package com.maplenou.backend.review;

import com.maplenou.backend.review.dto.*;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reviews", description = "Avis produits et boutiques")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── Avis produits ─────────────────────────────────────────────────────────

    @Operation(summary = "Soumettre un avis sur un produit (achat vérifié)")
    @PostMapping("/reviews/products")
    public ResponseEntity<ReviewResponse> createProductReview(
            @AuthenticationPrincipal User buyer,
            @Valid @RequestBody CreateProductReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createProductReview(buyer, request));
    }

    @Operation(summary = "Liste des avis publics d'un produit")
    @GetMapping("/products/{productId}/reviews")
    public Page<ReviewResponse> getProductReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getProductReviews(productId, pageable);
    }

    @Operation(summary = "Note moyenne et nombre d'avis d'un produit")
    @GetMapping("/products/{productId}/reviews/summary")
    public RatingSummaryResponse getProductRatingSummary(@PathVariable UUID productId) {
        return reviewService.getProductRatingSummary(productId);
    }

    // ── Avis boutiques ────────────────────────────────────────────────────────

    @Operation(summary = "Soumettre un avis sur une boutique (achat vérifié)")
    @PostMapping("/reviews/shops")
    public ResponseEntity<ReviewResponse> createShopReview(
            @AuthenticationPrincipal User buyer,
            @Valid @RequestBody CreateShopReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createShopReview(buyer, request));
    }

    @Operation(summary = "Liste des avis publics d'une boutique")
    @GetMapping("/shops/{shopId}/reviews")
    public Page<ReviewResponse> getShopReviews(
            @PathVariable UUID shopId,
            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getShopReviews(shopId, pageable);
    }

    @Operation(summary = "Note moyenne et nombre d'avis d'une boutique")
    @GetMapping("/shops/{shopId}/reviews/summary")
    public RatingSummaryResponse getShopRatingSummary(@PathVariable UUID shopId) {
        return reviewService.getShopRatingSummary(shopId);
    }

    // ── Mes avis ──────────────────────────────────────────────────────────────

    @Operation(summary = "Mes avis (produits et boutiques confondus)")
    @GetMapping("/users/me/reviews")
    @SecurityRequirement(name = "bearerAuth")
    public Page<MyReviewResponse> getMyReviews(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getMyReviews(currentUser, pageable);
    }
}
