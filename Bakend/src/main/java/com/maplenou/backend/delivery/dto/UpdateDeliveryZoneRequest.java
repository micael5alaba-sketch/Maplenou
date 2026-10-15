package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateDeliveryZoneRequest(
        @DecimalMin(value = "0", message = "Le tarif ne peut pas être négatif")
        BigDecimal deliveryFee,

        @DecimalMin(value = "0", message = "Le prix au km ne peut pas être négatif")
        BigDecimal pricePerKm,

        @Min(value = 1, message = "Le délai minimum est 1 minute")
        Integer estimatedTimeMinutes
) {}
