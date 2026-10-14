package com.maplenou.backend.seller.dto;

import com.maplenou.backend.seller.SellerFeeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplySellerRequest(
        @NotBlank @Size(min = 2, max = 100) String shopName,
        /** Optionnel — défaut STANDARD si absent (anciens clients mobiles pas encore mis à jour). */
        SellerFeeCategory feeCategory
) {
}
