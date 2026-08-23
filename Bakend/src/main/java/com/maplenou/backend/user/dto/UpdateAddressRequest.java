package com.maplenou.backend.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 100) String label,
        @Size(max = 100) String city,
        @Size(max = 100) String district,
        String details,
        Double latitude,
        Double longitude,
        Boolean isDefault
) {}
