package com.maplenou.backend.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateSupportTicketRequest(
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 200) String subject,
        @NotBlank String description,
        // Optionnel : la livraison concernée (côté livreur uniquement).
        UUID relatedDeliveryId,
        // Optionnel : URL Cloudinary d'une preuve (flux d'upload signé existant).
        @Size(max = 500) String photoUrl
) {
}
