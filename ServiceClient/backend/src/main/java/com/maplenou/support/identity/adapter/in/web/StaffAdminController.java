package com.maplenou.support.identity.adapter.in.web;

import com.maplenou.support.identity.application.StaffRoleService;
import com.maplenou.support.identity.domain.StaffAssignment;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Role;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Support roles of Maplenou accounts, given by phone number (DEC-CS10-17). */
@RestController
@Tag(name = "Administration")
public class StaffAdminController {

    private final StaffRoleService staff;
    private final CurrentActorProvider currentUser;

    public StaffAdminController(StaffRoleService staff, CurrentActorProvider currentUser) {
        this.staff = staff;
        this.currentUser = currentUser;
    }

    public record StaffRoleView(String phone, Role role, Instant assignedAt) {
        static StaffRoleView of(StaffAssignment a) {
            return new StaffRoleView(a.phone(), a.role(), a.assignedAt());
        }
    }

    public record StaffRoleUpdate(@NotBlank String phone, @NotNull Role role) {
    }

    @GetMapping("/api/v1/support/admin/staff-roles")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<StaffRoleView> list() {
        return staff.list().stream().map(StaffRoleView::of).toList();
    }

    @PutMapping("/api/v1/support/admin/staff-roles")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public StaffRoleView put(@Valid @RequestBody StaffRoleUpdate body) {
        return StaffRoleView.of(staff.assign(currentUser.get(), body.phone(), body.role()));
    }

    @DeleteMapping("/api/v1/support/admin/staff-roles/{phone}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String phone) {
        staff.remove(phone);
    }
}
