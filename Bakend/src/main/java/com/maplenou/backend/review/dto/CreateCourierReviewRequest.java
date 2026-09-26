package com.maplenou.backend.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCourierReviewRequest(

        /** La livraison terminée qui prouve le passage de ce livreur. */
        @NotNull
        UUID deliveryId,

        @NotNull
        @Min(1) @Max(5)
        Short rating,

        @Size(max = 2000)
        String comment
) {}
