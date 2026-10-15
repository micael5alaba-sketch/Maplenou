package com.maplenou.backend.maintenance;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.maintenance.dto.EnableMaintenanceRequest;
import com.maplenou.backend.maintenance.dto.MaintenanceStatusResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/maintenance")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Maintenance (Admin)")
@SecurityRequirement(name = "bearerAuth")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final AuditService auditService;

    @GetMapping
    public MaintenanceStatusResponse status() {
        return new MaintenanceStatusResponse(maintenanceService.isEnabled(), maintenanceService.getMessage());
    }

    /** Coupe tout le trafic API (sauf admin/auth/webhooks) jusqu'à /disable. */
    @PostMapping("/enable")
    public MaintenanceStatusResponse enable(@Valid @RequestBody EnableMaintenanceRequest request,
                                             @AuthenticationPrincipal User actor,
                                             HttpServletRequest httpRequest) {
        maintenanceService.enable(request.message());
        auditService.log(actor.getId(), AuditAction.MAINTENANCE_ENABLED,
                "PLATFORM", null, "Maintenance activée : " + maintenanceService.getMessage(),
                httpRequest.getRemoteAddr());
        return new MaintenanceStatusResponse(true, maintenanceService.getMessage());
    }

    @PostMapping("/disable")
    public MaintenanceStatusResponse disable(@AuthenticationPrincipal User actor,
                                              HttpServletRequest httpRequest) {
        maintenanceService.disable();
        auditService.log(actor.getId(), AuditAction.MAINTENANCE_DISABLED,
                "PLATFORM", null, "Maintenance désactivée", httpRequest.getRemoteAddr());
        return new MaintenanceStatusResponse(false, null);
    }
}
