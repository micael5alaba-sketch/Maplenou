package com.maplenou.backend.user.dto;

import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String email,
        Role role,
        boolean active,
        boolean phoneVerified,
        boolean totpEnabled,
        Instant createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getFullName(),
                u.getPhoneNumber(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.isPhoneVerified(),
                u.isTotpEnabled(),
                u.getCreatedAt()
        );
    }
}
