package com.maplenou.backend.order.dto;

import java.math.BigDecimal;

public record DeliveryAddressDto(
        String label,
        String city,
        String district,
        String details,
        BigDecimal latitude,
        BigDecimal longitude
) {}
