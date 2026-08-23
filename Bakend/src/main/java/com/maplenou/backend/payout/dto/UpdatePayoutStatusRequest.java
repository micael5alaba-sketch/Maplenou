package com.maplenou.backend.payout.dto;

import com.maplenou.backend.payout.PayoutStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePayoutStatusRequest(
        @NotNull PayoutStatus status,
        String notes
) {}
