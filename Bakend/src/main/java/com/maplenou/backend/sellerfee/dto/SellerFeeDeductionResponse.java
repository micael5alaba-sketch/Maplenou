package com.maplenou.backend.sellerfee.dto;

import com.maplenou.backend.sellerfee.SellerFeeDeduction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SellerFeeDeductionResponse(
        UUID id,
        LocalDate billingMonth,
        BigDecimal amount,
        Instant createdAt,
        UUID payoutId
) {
    public static SellerFeeDeductionResponse from(SellerFeeDeduction d) {
        return new SellerFeeDeductionResponse(
                d.getId(),
                d.getMonthlyFee().getBillingMonth(),
                d.getAmount(),
                d.getCreatedAt(),
                d.getPayout() != null ? d.getPayout().getId() : null);
    }
}
