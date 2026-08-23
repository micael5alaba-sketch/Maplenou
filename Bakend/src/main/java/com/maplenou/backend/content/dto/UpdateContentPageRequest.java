package com.maplenou.backend.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContentPageRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String body,
        boolean published
) {
}
