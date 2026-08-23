package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Plus de champ "role" : tout compte s'inscrit acheteur par defaut.
// Le profil vendeur s'active ensuite via POST /api/sellers/apply, sur le meme compte.
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,

        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
        String phoneNumber,

        @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule et un chiffre"
        )
        String password
) {
}
