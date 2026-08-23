package com.maplenou.backend.media.dto;

import jakarta.validation.constraints.Pattern;

public record UploadSignatureRequest(
        @Pattern(regexp = "^[a-zA-Z0-9/_-]{1,100}$", message = "Dossier invalide")
        String folder
) {
}
