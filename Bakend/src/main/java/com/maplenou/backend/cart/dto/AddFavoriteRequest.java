package com.maplenou.backend.cart.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddFavoriteRequest(
        @NotNull UUID productId
) {
}
