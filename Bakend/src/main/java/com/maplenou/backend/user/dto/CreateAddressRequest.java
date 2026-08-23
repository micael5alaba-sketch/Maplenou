package com.maplenou.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @Size(max = 100) String label,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String district,
        String details,
        Double latitude,
        Double longitude,
        boolean isDefault
) {}
