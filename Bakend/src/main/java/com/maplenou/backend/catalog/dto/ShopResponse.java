package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShopResponse(
        UUID id,
        UUID ownerId,
        String name,
        String slug,
        String description,
        String logoUrl,
        String coverUrl,
        ShopStatus status,
        String city,
        String district,
        String taxId,
        BigDecimal latitude,
        BigDecimal longitude,
        Instant createdAt,
        List<OpeningHourEntry> openingHours
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getOwner().getId(),
                shop.getName(),
                shop.getSlug(),
                shop.getDescription(),
                shop.getLogoUrl(),
                shop.getCoverUrl(),
                shop.getStatus(),
                shop.getCity(),
                shop.getDistrict(),
                shop.getTaxId(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getCreatedAt(),
                shop.getOpeningHours().stream().map(OpeningHourEntry::from).toList()
        );
    }
}
