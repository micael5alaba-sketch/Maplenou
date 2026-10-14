package com.maplenou.backend.courier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création directe d'un compte livreur par l'admin (cahier des charges §3.3, "option B" :
 * livreur interne à l'entreprise, dont les coordonnées sont transmises hors application).
 * Même règles de mot de passe qu'à l'inscription publique — l'admin le choisit et le
 * communique lui-même au livreur.
 */
public record CreateDeliveryAgentRequest(
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
