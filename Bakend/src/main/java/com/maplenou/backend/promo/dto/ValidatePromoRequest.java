package com.maplenou.backend.promo.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatePromoRequest(

        @NotBlank
        String code,

        /** Montant total du panier avant réduction. */
        @NotNull
        @DecimalMin("0.01")
        BigDecimal orderAmount,

        /** Identifiant de la boutique concernée (null pour codes PLATFORM). */
        UUID shopId
) {}
