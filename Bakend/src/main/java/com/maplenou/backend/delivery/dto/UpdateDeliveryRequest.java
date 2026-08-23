package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.delivery.ProofType;
import jakarta.validation.constraints.NotNull;

public record UpdateDeliveryRequest(
        @NotNull(message = "Le statut est obligatoire")
        DeliveryStatus status,

        /** Obligatoire si status = DELIVERED */
        ProofType proofType,

        /** URL Cloudinary de la photo ou signature (obligatoire si status = DELIVERED) */
        String proofUrl,

        String notes
) {}
