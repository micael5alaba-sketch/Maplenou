package com.maplenou.backend.content.dto;

import com.maplenou.backend.content.ContentPage;

import java.time.Instant;
import java.util.UUID;

public record ContentPageResponse(
        UUID id,
        String slug,
        String title,
        String body,
        boolean published,
        Instant createdAt,
        Instant updatedAt
) {
    public static ContentPageResponse from(ContentPage page) {
        return new ContentPageResponse(
                page.getId(), page.getSlug(), page.getTitle(), page.getBody(),
                page.isPublished(), page.getCreatedAt(), page.getUpdatedAt()
        );
    }
}
