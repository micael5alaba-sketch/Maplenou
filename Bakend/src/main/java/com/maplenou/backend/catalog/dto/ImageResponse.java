package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ProductImage;

import java.util.UUID;

public record ImageResponse(
        UUID id,
        String url,
        short position
) {
    public static ImageResponse from(ProductImage img) {
        return new ImageResponse(img.getId(), img.getUrl(), img.getPosition());
    }
}
