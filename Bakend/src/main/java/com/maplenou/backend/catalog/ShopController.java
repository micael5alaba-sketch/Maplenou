package com.maplenou.backend.catalog;

import com.maplenou.backend.catalog.dto.CreateShopRequest;
import com.maplenou.backend.catalog.dto.ShopResponse;
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

    // ----- Public -----

    @GetMapping("/api/shops/{slug}")
    public ShopResponse getBySlug(@PathVariable String slug) {
        return ShopResponse.from(shopService.getBySlug(slug));
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
