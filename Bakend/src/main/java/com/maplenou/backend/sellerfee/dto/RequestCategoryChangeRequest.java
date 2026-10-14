package com.maplenou.backend.sellerfee.dto;

import com.maplenou.backend.seller.SellerFeeCategory;
import jakarta.validation.constraints.NotNull;

public record RequestCategoryChangeRequest(
        @NotNull SellerFeeCategory requestedCategory
) {
}
