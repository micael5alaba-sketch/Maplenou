package com.maplenou.backend.staff;

import com.maplenou.backend.staff.dto.CreateStaffRequest;
import com.maplenou.backend.staff.dto.SetStaffActiveRequest;
import com.maplenou.backend.staff.dto.StaffCredentialsResponse;
import com.maplenou.backend.staff.dto.StaffResponse;
import com.maplenou.backend.staff.dto.UpdateStaffRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Gestion des comptes du personnel : Super Admin uniquement (permission STAFF_MANAGE). */
@RestController
@RequiredArgsConstructor
@Tag(name = "Personnel (Super Admin)")
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasAuthority('STAFF_MANAGE')")
@SecurityRequirement(name = "bearerAuth")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public Page<StaffResponse> list(@RequestParam(required = false) Boolean active,
                                    @PageableDefault(size = 20) Pageable pageable) {
        return staffService.list(active, pageable);
    }

    /**
     * Permissions de chaque rôle du personnel (source de vérité : RolePermissions). Sert au mode
     * « Voir en tant que » du Super Admin : l'interface affiche les menus et pages du rôle choisi.
     */
    @GetMapping("/roles")
    public java.util.Map<String, java.util.List<String>> rolePermissions() {
        java.util.Map<String, java.util.List<String>> result = new java.util.LinkedHashMap<>();
        for (com.maplenou.backend.user.Role role : com.maplenou.backend.user.Role.values()) {
            if (role.isStaff()) {
                result.put(role.name(), com.maplenou.backend.security.RolePermissions.forRole(role).stream()
                        .map(Enum::name).sorted().toList());
            }
        }
        return result;
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable UUID id) {
        return staffService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffCredentialsResponse create(@Valid @RequestBody CreateStaffRequest request,
                                           @AuthenticationPrincipal User actor,
                                           HttpServletRequest http) {
        return staffService.create(request, actor, http.getRemoteAddr());
    }

    @PatchMapping("/{id}")
    public StaffResponse update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateStaffRequest request,
                                @AuthenticationPrincipal User actor,
                                HttpServletRequest http) {
        return staffService.update(id, request, actor, http.getRemoteAddr());
    }

    @PatchMapping("/{id}/active")
    public StaffResponse setActive(@PathVariable UUID id,
                                   @Valid @RequestBody SetStaffActiveRequest request,
                                   @AuthenticationPrincipal User actor,
                                   HttpServletRequest http) {
        return staffService.setActive(id, request, actor, http.getRemoteAddr());
    }

    @PostMapping("/{id}/reset-password")
    public StaffCredentialsResponse resetPassword(@PathVariable UUID id,
                                                  @AuthenticationPrincipal User actor,
                                                  HttpServletRequest http) {
        return staffService.resetPassword(id, actor, http.getRemoteAddr());
    }
}
