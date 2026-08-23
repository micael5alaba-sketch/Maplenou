package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank String refreshToken,
        /** Access token à révoquer (optionnel mais recommandé). */
        String accessToken
) {}
