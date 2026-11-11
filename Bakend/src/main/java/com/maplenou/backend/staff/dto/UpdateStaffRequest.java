package com.maplenou.backend.staff.dto;

import com.maplenou.backend.user.Role;
import jakarta.validation.constraints.Size;

/** Modification d'un compte du personnel : tous les champs sont optionnels. */
public record UpdateStaffRequest(
        @Size(min = 2, max = 100) String fullName,
        Role role,
        Boolean deliveryPricingGranted
) {
}
