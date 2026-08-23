package com.maplenou.backend.promo.dto;

import com.maplenou.backend.promo.PromoCode;
import com.maplenou.backend.promo.PromoScopeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromoCodeResponse(
        UUID id,
        String code,
        PromoScopeType scopeType,
        UUID scopeId,
        BigDecimal discountPercent,
        BigDecimal minOrderAmount,
        Integer maxUses,
        int currentUses,
        Instant expiresAt,
        boolean active
) {
    public static PromoCodeResponse from(PromoCode p) {
        return new PromoCodeResponse(
                p.getId(),
                p.getCode(),
                p.getScopeType(),
                p.getScopeId(),
                p.getDiscountPercent(),
                p.getMinOrderAmount(),
                p.getMaxUses(),
                p.getCurrentUses(),
                p.getExpiresAt(),
                p.isActive()
        );
    }
}
