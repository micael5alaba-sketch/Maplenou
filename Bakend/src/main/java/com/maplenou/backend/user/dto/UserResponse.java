package com.maplenou.backend.user.dto;

import com.maplenou.backend.seller.SellerStatus;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String email,
        String avatarUrl,
        Role role,
        boolean phoneVerified,
        SellerStatus sellerStatus,
        /** Permissions du back-office (vide pour un acheteur/vendeur/livreur). */
        java.util.List<String> permissions,
        boolean mustChangePassword,
        boolean totpEnabled
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole(),
                user.isPhoneVerified(),
                user.getSellerProfile() != null ? user.getSellerProfile().getStatus() : null,
                user.permissions().stream().map(Enum::name).sorted().toList(),
                user.isMustChangePassword(),
                user.isTotpEnabled()
        );
    }
}
