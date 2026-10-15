package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String phoneNumber,

        @NotBlank(message = "Le code est obligatoire")
        @Pattern(regexp = "^[0-9]{6}$", message = "Le code doit contenir 6 chiffres")
        String code,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
        )
        String newPassword
) {
}
