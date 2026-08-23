package com.maplenou.backend.seller;

import com.maplenou.backend.seller.dto.ApplySellerRequest;
import com.maplenou.backend.seller.dto.SellerProfileResponse;
import com.maplenou.backend.seller.dto.UpdateSellerStatusRequest;
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
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
@Tag(name = "Profil vendeur")
@SecurityRequirement(name = "bearerAuth")
public class SellerProfileController {

    private final SellerProfileService sellerProfileService;

    // Un compte deja connecte (acheteur) active son profil vendeur ici, sans nouvelle inscription.
    @PostMapping("/apply")
    public ResponseEntity<SellerProfileResponse> apply(@AuthenticationPrincipal User currentUser,
                                                         @Valid @RequestBody ApplySellerRequest request) {
        SellerProfile created = sellerProfileService.apply(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SellerProfileResponse.from(created));
    }

    @GetMapping("/me")
    public SellerProfileResponse getMine(@AuthenticationPrincipal User currentUser) {
        return SellerProfileResponse.from(sellerProfileService.getMine(currentUser));
    }

    // ----- Admin -----

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SellerProfileResponse> listAll(
            @RequestParam(required = false) SellerStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return sellerProfileService.listByStatus(status, pageable)
                .map(SellerProfileResponse::from);
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public SellerProfileResponse updateStatus(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateSellerStatusRequest request,
                                               @AuthenticationPrincipal User currentUser,
                                               HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return SellerProfileResponse.from(
                sellerProfileService.updateStatus(id, request, currentUser.getId(), ip));
    }
}
