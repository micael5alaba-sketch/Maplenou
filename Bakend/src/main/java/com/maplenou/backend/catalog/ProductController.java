package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.*;
import com.maplenou.backend.common.CursorPage;
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

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Produits")
public class ProductController {

    private final ProductService productService;

    // ----- Vendeur : gestion de ses produits -----

    @PostMapping("/api/shops/mine/products")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductDetailResponse> create(@AuthenticationPrincipal User currentUser,
                                                         @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(currentUser, request));
    }

    @GetMapping("/api/shops/mine/products")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public Page<ProductSummaryResponse> listMine(@AuthenticationPrincipal User currentUser,
                                                  @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return productService.listMine(currentUser, pageable);
    }

    @GetMapping("/api/shops/mine/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ProductDetailResponse getMine(@AuthenticationPrincipal User currentUser,
                                          @PathVariable UUID id) {
        return productService.getMineDetail(currentUser, id);
    }

    @PatchMapping("/api/shops/mine/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ProductDetailResponse update(@AuthenticationPrincipal User currentUser,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(currentUser, id, request);
    }

    @DeleteMapping("/api/shops/mine/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        productService.softDelete(currentUser, id);
    }

    // ----- Vendeur : variantes -----

    @PostMapping("/api/shops/mine/products/{productId}/variants")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<VariantResponse> addVariant(@AuthenticationPrincipal User currentUser,
                                                       @PathVariable UUID productId,
                                                       @Valid @RequestBody CreateVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addVariant(currentUser, productId, request));
    }

    @PatchMapping("/api/shops/mine/products/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public VariantResponse updateVariant(@AuthenticationPrincipal User currentUser,
                                          @PathVariable UUID productId,
                                          @PathVariable UUID variantId,
                                          @Valid @RequestBody UpdateVariantRequest request) {
        return productService.updateVariant(currentUser, productId, variantId, request);
    }

    @DeleteMapping("/api/shops/mine/products/{productId}/variants/{variantId}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@AuthenticationPrincipal User currentUser,
                               @PathVariable UUID productId,
                               @PathVariable UUID variantId) {
        productService.deleteVariant(currentUser, productId, variantId);
    }

    // ----- Vendeur : images -----

    @PostMapping("/api/shops/mine/products/{productId}/images")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ImageResponse> addImage(@AuthenticationPrincipal User currentUser,
                                                   @PathVariable UUID productId,
                                                   @Valid @RequestBody AddImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addImage(currentUser, productId, request));
    }

    @DeleteMapping("/api/shops/mine/products/{productId}/images/{imageId}")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@AuthenticationPrincipal User currentUser,
                             @PathVariable UUID productId,
                             @PathVariable UUID imageId) {
        productService.deleteImage(currentUser, productId, imageId);
    }

    // ----- Public : catalogue -----

    @GetMapping("/api/products")
    public CursorPage<ProductSummaryResponse> listCatalog(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        return productService.listCatalog(categoryId, shopId, minPrice, maxPrice, cursor, clampedSize);
    }

    @GetMapping("/api/products/{slug}")
    public ProductDetailResponse getBySlug(@PathVariable String slug) {
        return productService.getDetailBySlug(slug);
    }
}
