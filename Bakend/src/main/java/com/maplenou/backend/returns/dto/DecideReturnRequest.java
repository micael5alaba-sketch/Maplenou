package com.maplenou.backend.returns.dto;

import com.maplenou.backend.returns.ReturnDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecideReturnRequest(
        @NotNull(message = "La décision est obligatoire (APPROVED ou REJECTED)")
        ReturnDecision decision,

        @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
        String adminNotes
) {}
