package com.maplenou.backend.payout.dto;

import com.maplenou.backend.payout.PayoutMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePayoutMethodRequest(
        @NotNull PayoutMethodType type,
        @NotBlank @Size(max = 50) String accountNumber
) {
}
