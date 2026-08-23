package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ShopStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateShopStatusRequest(
        @NotNull ShopStatus status
) {
}
