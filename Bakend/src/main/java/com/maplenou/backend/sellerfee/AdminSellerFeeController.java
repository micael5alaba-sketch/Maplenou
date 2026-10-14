package com.maplenou.backend.sellerfee;

import com.maplenou.backend.sellerfee.dto.DecideCategoryChangeRequest;
import com.maplenou.backend.sellerfee.dto.SellerFeeCategoryChangeResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Frais vendeurs")
public class AdminSellerFeeController {

    private final SellerFeeService sellerFeeService;

    /** GET /api/admin/seller-fees/category-changes — demandes en attente (ou filtrées par statut). */
    @GetMapping("/api/admin/seller-fees/category-changes")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<SellerFeeCategoryChangeResponse> listCategoryChangeRequests(
            @RequestParam(required = false) SellerFeeCategoryChangeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return sellerFeeService.listCategoryChangeRequests(status, pageable)
                .map(SellerFeeCategoryChangeResponse::from);
    }

    /** PATCH /api/admin/seller-fees/category-changes/{id}/decide — approuver/rejeter. */
    @PatchMapping("/api/admin/seller-fees/category-changes/{id}/decide")
    @PreAuthorize("hasRole('ADMIN')")
    public SellerFeeCategoryChangeResponse decide(
            @PathVariable UUID id,
            @Valid @RequestBody DecideCategoryChangeRequest request,
            @AuthenticationPrincipal User actor,
            HttpServletRequest httpRequest) {
        var decided = sellerFeeService.decideCategoryChange(
                id, request.approve(), request.adminNotes(), actor, httpRequest.getRemoteAddr());
        return SellerFeeCategoryChangeResponse.from(decided);
    }
}
