package com.maplenou.backend.sellerfee.dto;

import jakarta.validation.constraints.NotNull;

public record DecideCategoryChangeRequest(
        @NotNull Boolean approve,
        String adminNotes
) {
}
