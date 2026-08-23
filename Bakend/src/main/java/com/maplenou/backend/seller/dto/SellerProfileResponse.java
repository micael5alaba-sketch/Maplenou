package com.maplenou.backend.seller.dto;

import com.maplenou.backend.seller.SellerProfile;
import com.maplenou.backend.seller.SellerStatus;

import java.util.UUID;

public record SellerProfileResponse(
        UUID id,
        String shopName,
        SellerStatus status
) {
    public static SellerProfileResponse from(SellerProfile profile) {
        return new SellerProfileResponse(profile.getId(), profile.getShopName(), profile.getStatus());
    }
}
