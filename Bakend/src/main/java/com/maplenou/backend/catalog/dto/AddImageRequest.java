package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddImageRequest(
        @NotBlank @Size(max = 500) String url,
        @Min(0) short position
) {
}
