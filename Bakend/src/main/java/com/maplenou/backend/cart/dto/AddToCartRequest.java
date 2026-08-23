package com.maplenou.backend.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartRequest(
        @NotNull UUID productVariantId,
        @NotNull @Min(1) Integer quantity
) {
}
