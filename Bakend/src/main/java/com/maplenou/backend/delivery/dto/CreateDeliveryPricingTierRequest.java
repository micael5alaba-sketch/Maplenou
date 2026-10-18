package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateDeliveryPricingTierRequest(
        // Nul = palier "au-delà de X km" (illimité) — au plus un par barème.
        @DecimalMin(value = "0.01", message = "La distance maximale doit être positive") BigDecimal maxDistanceKm,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {
}
