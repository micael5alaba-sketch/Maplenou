package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank String phoneNumber
) {
}
