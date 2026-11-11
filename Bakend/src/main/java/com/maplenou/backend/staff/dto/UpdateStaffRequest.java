package com.maplenou.backend.staff.dto;

import com.maplenou.backend.user.Role;
import jakarta.validation.constraints.Size;

/** Modification d'un compte du personnel : tous les champs sont optionnels. */
public record UpdateStaffRequest(
        @Size(min = 2, max = 100) String fullName,
        /** Nouvel email (identifiant de connexion) : modifié uniquement par le Super Admin. */
        @jakarta.validation.constraints.Email @Size(max = 255) String email,
        /** Nouveau numéro de téléphone (identifiant de connexion) : modifié uniquement par le Super Admin. */
        @jakarta.validation.constraints.Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
        String phoneNumber,
        Role role,
        Boolean deliveryPricingGranted
) {
}
