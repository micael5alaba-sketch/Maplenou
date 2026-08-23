package com.maplenou.backend.review.dto;

public record RatingSummaryResponse(
        double averageRating,
        long reviewCount
) {}
