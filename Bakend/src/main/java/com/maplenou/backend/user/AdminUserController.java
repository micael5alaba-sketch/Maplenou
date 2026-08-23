package com.maplenou.backend.user;

import com.maplenou.backend.user.dto.AdminUserResponse;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        return userService.listUsers(role, active, pageable).map(AdminUserResponse::from);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable UUID id) {
        return AdminUserResponse.from(userService.getById(id));
    }

    @PatchMapping("/{id}/ban")
    public AdminUserResponse ban(@PathVariable UUID id,
                                  @AuthenticationPrincipal User actor,
                                  HttpServletRequest request) {
        return AdminUserResponse.from(
                userService.banUser(id, actor.getId(), request.getRemoteAddr()));
    }

    @PatchMapping("/{id}/unban")
    public AdminUserResponse unban(@PathVariable UUID id,
                                    @AuthenticationPrincipal User actor,
                                    HttpServletRequest request) {
        return AdminUserResponse.from(
                userService.unbanUser(id, actor.getId(), request.getRemoteAddr()));
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse changeRole(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeRoleRequest roleRequest,
                                         @AuthenticationPrincipal User actor,
                                         HttpServletRequest request) {
        return AdminUserResponse.from(
                userService.changeRole(id, roleRequest.role(), actor.getId(), request.getRemoteAddr()));
    }
}
