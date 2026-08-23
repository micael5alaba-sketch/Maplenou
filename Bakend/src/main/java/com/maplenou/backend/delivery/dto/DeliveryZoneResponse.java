package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.DeliveryZone;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryZoneResponse(
        UUID id,
        String name,
        BigDecimal deliveryFee,
        int estimatedTimeMinutes,
        boolean active
) {
    public static DeliveryZoneResponse from(DeliveryZone z) {
        return new DeliveryZoneResponse(
                z.getId(),
                z.getName(),
                z.getDeliveryFee(),
                z.getEstimatedTimeMinutes(),
                z.isActive()
        );
    }
}
