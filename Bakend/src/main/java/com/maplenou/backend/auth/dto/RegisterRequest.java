package com.maplenou.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Compte acheteur par defaut. Le profil vendeur s'active ensuite via POST /api/sellers/apply,
// sur le meme compte. Un livreur independant peut en revanche choisir ce role des l'inscription
// (accountType="DELIVERY_AGENT") : seule cette valeur est acceptee ici, jamais "ADMIN" (voir
// AuthService.register - l'enum Role existe cote serveur mais ce champ n'autorise qu'un sous-
// ensemble volontairement restreint, verifie explicitement, independamment de ce que l'enum
// accepterait par ailleurs).
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
        String password,

        // Optionnel. Seule valeur acceptee : "DELIVERY_AGENT". Toute autre valeur (y compris
        // "ADMIN") est rejetee par AuthService.register, quelle que soit l'enum Role cote serveur.
        String accountType
) {
}
