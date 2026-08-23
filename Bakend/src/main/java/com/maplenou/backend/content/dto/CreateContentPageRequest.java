package com.maplenou.backend.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateContentPageRequest(
        @NotBlank @Size(max = 150) @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug invalide (minuscules, chiffres, tirets)")
        String slug,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        boolean published
) {
}
