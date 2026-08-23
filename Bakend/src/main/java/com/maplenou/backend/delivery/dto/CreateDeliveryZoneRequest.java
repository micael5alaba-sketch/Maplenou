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

        @Min(value = 1, message = "Le délai minimum est 1 minute")
        int estimatedTimeMinutes
) {}
