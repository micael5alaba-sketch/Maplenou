package com.maplenou.backend.sellerfee.dto;

import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.sellerfee.SellerMonthlyFee;
import com.maplenou.backend.sellerfee.SellerMonthlyFeeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SellerMonthlyFeeResponse(
        UUID id,
        LocalDate billingMonth,
        SellerFeeCategory category,
        BigDecimal amount,
        BigDecimal amountPaid,
        SellerMonthlyFeeStatus status
) {
    public static SellerMonthlyFeeResponse from(SellerMonthlyFee fee) {
        return new SellerMonthlyFeeResponse(
                fee.getId(), fee.getBillingMonth(), fee.getCategory(),
                fee.getAmount(), fee.getAmountPaid(), fee.getStatus());
    }
}
