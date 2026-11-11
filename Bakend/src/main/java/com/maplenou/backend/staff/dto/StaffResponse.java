package com.maplenou.backend.staff.dto;

import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;

import java.time.Instant;
import java.util.UUID;

public record StaffResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String email,
        Role role,
        boolean active,
        boolean totpEnabled,
        boolean mustChangePassword,
        boolean deliveryPricingGranted,
        Instant createdAt
) {
    public static StaffResponse from(User u) {
        return new StaffResponse(
                u.getId(),
                u.getFullName(),
                u.getPhoneNumber(),
                u.getEmail(),
                u.getRole(),
                u.isActive(),
                u.isTotpEnabled(),
                u.isMustChangePassword(),
                u.isDeliveryPricingGranted(),
                u.getCreatedAt()
        );
    }
}
