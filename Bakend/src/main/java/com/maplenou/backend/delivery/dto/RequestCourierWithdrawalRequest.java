package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Demande de retrait d'un livreur : coordonnées de paiement (ex. "T-Money +22890123456"). */
public record RequestCourierWithdrawalRequest(
        @NotBlank @Size(min = 5, max = 100) String paymentDetails
) {
}
