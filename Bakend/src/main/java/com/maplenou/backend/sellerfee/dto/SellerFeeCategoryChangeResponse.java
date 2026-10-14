package com.maplenou.backend.sellerfee.dto;

import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.sellerfee.SellerFeeCategoryChangeRequest;
import com.maplenou.backend.sellerfee.SellerFeeCategoryChangeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SellerFeeCategoryChangeResponse(
        UUID id,
        String shopName,
        SellerFeeCategory requestedCategory,
        SellerFeeCategoryChangeStatus status,
        LocalDate effectiveFrom,
        Instant createdAt,
        Instant decidedAt
) {
    public static SellerFeeCategoryChangeResponse from(SellerFeeCategoryChangeRequest r) {
        return new SellerFeeCategoryChangeResponse(
                r.getId(),
                r.getSellerProfile().getShopName(),
                r.getRequestedCategory(),
                r.getStatus(),
                r.getEffectiveFrom(),
                r.getCreatedAt(),
                r.getDecidedAt());
    }
}
