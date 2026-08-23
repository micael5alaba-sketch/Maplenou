package com.maplenou.backend.review.dto;

import com.maplenou.backend.review.ProductReview;
import com.maplenou.backend.review.ShopReview;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        AuthorSummary author,
        short rating,
        String comment,
        Instant createdAt
) {

    public record AuthorSummary(UUID id, String fullName) {}

    public static ReviewResponse from(ProductReview r) {
        return new ReviewResponse(
                r.getId(),
                new AuthorSummary(r.getAuthor().getId(), r.getAuthor().getFullName()),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }

    public static ReviewResponse from(ShopReview r) {
        return new ReviewResponse(
                r.getId(),
                new AuthorSummary(r.getAuthor().getId(), r.getAuthor().getFullName()),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt()
        );
    }
}
