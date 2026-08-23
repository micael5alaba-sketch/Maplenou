package com.maplenou.backend.auth.dto;
import com.maplenou.backend.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
