package com.maplenou.backend.courier.dto;

import com.maplenou.backend.courier.CourierDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SubmitCourierDocumentRequest(
        @NotNull CourierDocumentType type,
        @NotBlank String fileUrl,
        /** Optionnelle : ex. une pièce d'identité n'a pas toujours de date d'expiration. */
        LocalDate expiresAt
) {
}
