package com.maplenou.backend.admin;

import com.maplenou.backend.admin.dto.RoleDashboardResponse;
import com.maplenou.backend.security.Permission;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord dédié au rôle de l'utilisateur connecté (logistique, support, retraits, audit). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Tableaux de bord par rôle")
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final RoleDashboardService roleDashboardService;

    @GetMapping
    public RoleDashboardResponse get(@AuthenticationPrincipal User viewer,
                                     @RequestParam(required = false) Role viewAs) {
        // « Voir en tant que » : réservé au Super Admin, simple aperçu d'un autre rôle.
        if (viewAs != null && !viewer.permissions().contains(Permission.STAFF_MANAGE)) {
            throw new AccessDeniedException("Réservé au Super Admin");
        }
        return roleDashboardService.get(viewer, viewAs);
    }
}
