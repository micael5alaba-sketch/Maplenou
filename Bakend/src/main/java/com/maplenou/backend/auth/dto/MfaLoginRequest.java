package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaLoginRequest(
        @NotBlank String mfaToken,
        @NotBlank String code
) {
}
