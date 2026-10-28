package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.AdminCreateShopRequest;
import com.maplenou.backend.catalog.dto.CreateShopRequest;
import com.maplenou.backend.catalog.dto.ProductSummaryResponse;
import com.maplenou.backend.common.CursorPage;
import com.maplenou.backend.catalog.dto.ShopResponse;
import com.maplenou.backend.catalog.dto.UpdateOpeningHoursRequest;
import com.maplenou.backend.catalog.dto.UpdateShopRequest;
import com.maplenou.backend.catalog.dto.UpdateShopStatusRequest;
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
@Tag(name = "Boutiques")
public class ShopController {

    private final ShopService shopService;
    private final ProductService productService;

    // ----- Vendeur -----

    @PostMapping("/api/shops")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ShopResponse> create(@AuthenticationPrincipal User currentUser,
                                                @Valid @RequestBody CreateShopRequest request) {
        Shop shop = shopService.create(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopResponse.from(shop));
    }

    @GetMapping("/api/shops/mine")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ShopResponse getMine(@AuthenticationPrincipal User currentUser) {
        return ShopResponse.from(shopService.getMine(currentUser));
    }

    @PatchMapping("/api/shops/mine")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ShopResponse updateMine(@AuthenticationPrincipal User currentUser,
                                    @Valid @RequestBody UpdateShopRequest request) {
        return ShopResponse.from(shopService.updateMine(currentUser, request));
    }

    /** Remplace l'horaire d'ouverture complet (au plus 7 entrées, une par jour) — signalé
     * manquant dans le rapport de modification du 28/10/2026. */
    @PutMapping("/api/shops/mine/opening-hours")
    @PreAuthorize("hasRole('SELLER')")
    @SecurityRequirement(name = "bearerAuth")
    public ShopResponse updateOpeningHours(@AuthenticationPrincipal User currentUser,
                                            @Valid @RequestBody UpdateOpeningHoursRequest request) {
        return ShopResponse.from(shopService.updateOpeningHours(currentUser, request));
    }

    // ----- Public -----

    /** Annuaire public des boutiques (approuvées uniquement) — absent jusqu'ici, signalé par
     * l'audit externe du 16/10/2026 : aucun moyen de parcourir les boutiques sans déjà
     * connaître leur slug. Faible volume (back-office/catalogue boutiques), Pageable standard
     * suffit (voir CLAUDE.md §9). */
    @GetMapping("/api/shops")
    public Page<ShopResponse> listPublic(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return shopService.listPublic(pageable).map(ShopResponse::from);
    }

    @GetMapping("/api/shops/{slug}")
    public ShopResponse getBySlug(@PathVariable String slug) {
        return ShopResponse.from(shopService.getBySlug(slug));
    }

    /** Alias pratique vers le catalogue filtré par boutique (équivalent à
     * GET /api/products?shopId=...), pour les consommateurs d'API qui s'attendent à une route
     * imbriquée classique — les deux fonctionnent, celle-ci résout juste le slug en UUID. */
    @GetMapping("/api/shops/{slug}/products")
    public CursorPage<ProductSummaryResponse> listProductsByShop(
            @PathVariable String slug,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        UUID shopId = shopService.getBySlug(slug).getId();
        int clampedSize = Math.min(Math.max(size, 1), 100);
        return productService.listCatalog(null, shopId, null, null, null, cursor, clampedSize);
    }

    // ----- Admin -----

    @GetMapping("/api/admin/shops")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public Page<ShopResponse> listAll(@RequestParam(required = false) ShopStatus status,
                                       @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        if (status != null) {
            return shopService.listByStatus(status, pageable).map(ShopResponse::from);
        }
        return shopService.listAll(pageable).map(ShopResponse::from);
    }

    /** Admin : crée directement une boutique pour un vendeur déjà approuvé (sans passer par le vendeur). */
    @PostMapping("/api/admin/shops")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ShopResponse> adminCreate(@Valid @RequestBody AdminCreateShopRequest request,
                                                      @AuthenticationPrincipal User currentUser,
                                                      HttpServletRequest httpRequest) {
        Shop shop = shopService.adminCreate(request, currentUser.getId(), resolveIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ShopResponse.from(shop));
    }

    @PatchMapping("/api/admin/shops/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ShopResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateShopStatusRequest request,
                                      @AuthenticationPrincipal User currentUser,
                                      HttpServletRequest httpRequest) {
        String ip = resolveIp(httpRequest);
        return ShopResponse.from(shopService.updateStatus(currentUser.getId(), id, request.status(), ip));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
