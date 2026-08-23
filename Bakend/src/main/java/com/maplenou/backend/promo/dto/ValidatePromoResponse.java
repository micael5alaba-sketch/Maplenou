package com.maplenou.backend.promo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatePromoResponse(
        UUID promoCodeId,
        String code,
        BigDecimal discountPercent,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}
