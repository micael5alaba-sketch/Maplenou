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
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.maplenou.backend.user.User viewer) {
        return adminKpiService.getDashboard(period, viewer);
    }
}
