package com.maplenou.backend.seller.dto;

import com.maplenou.backend.seller.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerStatusRequest(
        @NotNull SellerStatus status,
        String adminNote
) {}
