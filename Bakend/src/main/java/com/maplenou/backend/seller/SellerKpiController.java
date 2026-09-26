package com.maplenou.backend.seller;

import com.maplenou.backend.seller.dto.SellerKpiResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sellers/me/kpis")
@PreAuthorize("hasRole('SELLER')")
@RequiredArgsConstructor
@Tag(name = "KPIs Vendeur")
@SecurityRequirement(name = "bearerAuth")
public class SellerKpiController {

    private final SellerKpiService sellerKpiService;

    @GetMapping
    public SellerKpiResponse getDashboard(@AuthenticationPrincipal User currentUser) {
        return sellerKpiService.getDashboard(currentUser);
    }
}
