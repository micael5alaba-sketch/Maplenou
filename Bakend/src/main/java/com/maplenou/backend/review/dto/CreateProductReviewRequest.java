package com.maplenou.backend.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductReviewRequest(

        @NotNull
        UUID productId,

        /** La sous-commande livrée qui prouve l'achat. */
        @NotNull
        UUID subOrderId,

        @NotNull
        @Min(1) @Max(5)
        Short rating,

        @Size(max = 2000)
        String comment
) {}
