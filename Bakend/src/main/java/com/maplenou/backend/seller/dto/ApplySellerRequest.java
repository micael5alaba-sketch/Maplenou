package com.maplenou.backend.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplySellerRequest(
        @NotBlank @Size(min = 2, max = 100) String shopName
) {
}
