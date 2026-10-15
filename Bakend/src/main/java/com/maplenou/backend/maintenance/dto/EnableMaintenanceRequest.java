package com.maplenou.backend.maintenance.dto;

import jakarta.validation.constraints.Size;

public record EnableMaintenanceRequest(
        @Size(max = 500) String message
) {
}
