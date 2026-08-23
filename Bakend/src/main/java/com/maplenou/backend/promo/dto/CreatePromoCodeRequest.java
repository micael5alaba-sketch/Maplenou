package com.maplenou.backend.promo.dto;

import com.maplenou.backend.promo.PromoScopeType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreatePromoCodeRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Le code ne doit contenir que des lettres majuscules, chiffres, tirets ou underscores")
        String code,

        @NotNull
        PromoScopeType scopeType,

        /** Obligatoire si scopeType = SHOP. */
        UUID scopeId,

        @NotNull
        @DecimalMin("1.00") @DecimalMax("100.00")
        BigDecimal discountPercent,

        @DecimalMin("0.00")
        BigDecimal minOrderAmount,

        @Min(1)
        Integer maxUses,

        Instant expiresAt
) {}
