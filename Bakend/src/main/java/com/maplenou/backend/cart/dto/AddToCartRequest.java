package com.maplenou.backend.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull UUID productVariantId,
        // Max 1000 : au-delà, la vérification de stock rejetterait de toute façon la plupart des
        // cas réels, mais borner ici évite qu'une valeur extrême (sans dépasser Integer.MAX_VALUE)
        // n'atteigne les calculs de commission/total (signalé par l'audit externe du 16/10/2026).
        @NotNull @Min(1) @Max(1000) Integer quantity
) {
}
