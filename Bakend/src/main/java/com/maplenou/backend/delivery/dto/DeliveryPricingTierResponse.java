package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.DeliveryPricingTier;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryPricingTierResponse(
        UUID id,
        BigDecimal maxDistanceKm,
        BigDecimal price
) {
    public static DeliveryPricingTierResponse from(DeliveryPricingTier tier) {
        return new DeliveryPricingTierResponse(tier.getId(), tier.getMaxDistanceKm(), tier.getPrice());
    }
}
