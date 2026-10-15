package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDeliveryZoneRequest(
        @NotBlank(message = "Le nom de la zone est obligatoire")
        String name,

        @NotNull(message = "Le tarif de livraison est obligatoire")
        @DecimalMin(value = "0", message = "Le tarif ne peut pas être négatif")
        BigDecimal deliveryFee,

        /** Prix au km au-delà du tarif de base, pour un calcul dynamique (type Gozem).
         * Optionnel : 0 = tarif fixe, comportement historique. */
        @DecimalMin(value = "0", message = "Le prix au km ne peut pas être négatif")
        BigDecimal pricePerKm,

        @Min(value = 1, message = "Le délai minimum est 1 minute")
        int estimatedTimeMinutes
) {}
