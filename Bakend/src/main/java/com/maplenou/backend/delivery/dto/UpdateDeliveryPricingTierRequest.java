package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// Remplacement complet (pas un patch partiel) : les deux champs sont toujours appliqués
// tels quels, maxDistanceKm nul inclus (= transforme ce palier en palier "illimité").
public record UpdateDeliveryPricingTierRequest(
        @DecimalMin(value = "0.01", message = "La distance maximale doit être positive") BigDecimal maxDistanceKm,
        @NotNull @DecimalMin("0.00") BigDecimal price
) {
}
