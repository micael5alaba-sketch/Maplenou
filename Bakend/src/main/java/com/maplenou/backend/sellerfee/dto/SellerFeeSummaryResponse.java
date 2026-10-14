package com.maplenou.backend.sellerfee.dto;

import com.maplenou.backend.seller.SellerFeeCategory;

import java.math.BigDecimal;

public record SellerFeeSummaryResponse(
        SellerFeeCategory category,
        BigDecimal monthlyFeeAmount,
        BigDecimal unpaidFeesTotal,
        BigDecimal availableBalance,
        BigDecimal estimatedNetPayout,
        /** Catégorie demandée dans une demande encore en attente de décision admin — null sinon. */
        SellerFeeCategory pendingCategoryChangeRequested
) {
}
