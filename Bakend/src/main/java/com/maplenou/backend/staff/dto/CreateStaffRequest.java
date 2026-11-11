package com.maplenou.backend.staff.dto;

import com.maplenou.backend.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Création d'un compte du personnel par le Super Admin (jamais par inscription publique). Le mot
 * de passe initial est généré côté serveur et renvoyé une seule fois au créateur.
 */
public record CreateStaffRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
        String phoneNumber,
        /** Email du compte : identifiant de connexion du personnel (avec le mot de passe). */
        @NotBlank @jakarta.validation.constraints.Email @Size(max = 255) String email,
        @NotNull Role role,
        /** Tarification de livraison attribuée (uniquement pertinent pour l'Administrateur des livraisons). */
        Boolean deliveryPricingGranted
) {
}
