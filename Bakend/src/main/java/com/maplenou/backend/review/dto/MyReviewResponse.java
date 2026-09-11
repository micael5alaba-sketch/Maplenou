package com.maplenou.backend.review.dto;

import java.time.Instant;
import java.util.UUID;

/** Avis écrit par l'utilisateur connecté, produits et boutiques confondus. */
public record MyReviewResponse(
        UUID id,
        String type,        // "PRODUCT" ou "SHOP"
        UUID targetId,
        String targetName,
        int rating,
        String comment,
        Instant createdAt
) {
    public static MyReviewResponse from(MyReviewRow row) {
        return new MyReviewResponse(
                row.getId(),
                row.getType(),
                row.getTargetId(),
                row.getTargetName(),
                row.getRating(),
                row.getComment(),
                row.getCreatedAt()
        );
    }
}
