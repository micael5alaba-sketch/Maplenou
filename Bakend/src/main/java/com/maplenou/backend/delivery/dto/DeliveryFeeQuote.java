package com.maplenou.backend.delivery.dto;

import java.math.BigDecimal;

public record DeliveryFeeQuote(
        BigDecimal fee,
        BigDecimal distanceKm,
        String zoneName
) {
}
