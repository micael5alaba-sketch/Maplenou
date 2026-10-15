package com.maplenou.backend.seller.dto;

import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.seller.SellerProfile;
import com.maplenou.backend.seller.SellerStatus;

import java.time.Instant;
import java.util.UUID;

public record SellerProfileResponse(
        UUID id,
        UUID userId,
        String userFullName,
        String userPhoneNumber,
        String shopName,
        SellerStatus status,
        SellerFeeCategory feeCategory,
        Instant createdAt
) {
    public static SellerProfileResponse from(SellerProfile profile) {
        return new SellerProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getPhoneNumber(),
                profile.getShopName(),
                profile.getStatus(),
                profile.getFeeCategory(),
                profile.getCreatedAt());
    }
}
