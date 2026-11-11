package com.maplenou.backend.admin;

import com.maplenou.backend.admin.dto.DashboardKpiResponse;
import com.maplenou.backend.admin.dto.KpiPeriod;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/kpis")
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
@RequiredArgsConstructor
@Tag(name = "KPIs Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminKpiController {

    private final AdminKpiService adminKpiService;

    @GetMapping
    public DashboardKpiResponse getDashboard(
            @RequestParam(defaultValue = "LAST_30_DAYS") KpiPeriod period,
            @RequestParam(required = false) com.maplenou.backend.user.Role viewAs,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.maplenou.backend.user.User viewer) {
        // « Voir en tant que » : réservé au Super Admin (permission STAFF_MANAGE), simple aperçu du
        // tableau de bord d'un autre rôle — aucune donnée au-delà de ce que le Super Admin voit déjà.
        if (viewAs != null && !viewer.permissions().contains(com.maplenou.backend.security.Permission.STAFF_MANAGE)) {
            throw new org.springframework.security.access.AccessDeniedException("Réservé au Super Admin");
        }
        return adminKpiService.getDashboard(period, viewer, viewAs);
    }
}
