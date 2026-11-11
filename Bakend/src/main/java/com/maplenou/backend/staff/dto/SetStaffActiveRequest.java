package com.maplenou.backend.staff.dto;

import jakarta.validation.constraints.NotNull;

public record SetStaffActiveRequest(
        @NotNull Boolean active,
        /** Motif (enregistré dans le journal d'audit). */
        String justification
) {
}
