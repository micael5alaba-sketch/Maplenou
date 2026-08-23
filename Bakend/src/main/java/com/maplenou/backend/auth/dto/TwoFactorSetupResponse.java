package com.maplenou.backend.auth.dto;

public record TwoFactorSetupResponse(
        String secret,
        String otpAuthUri
) {
}
