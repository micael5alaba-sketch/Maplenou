package com.maplenou.backend.user.dto;

import com.maplenou.backend.user.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @NotNull Role role
) {}
