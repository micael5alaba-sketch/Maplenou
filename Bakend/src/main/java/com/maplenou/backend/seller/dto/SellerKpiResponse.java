package com.maplenou.backend.seller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Agrégats du dashboard vendeur (`GET /api/sellers/me/kpis`) — équivalent
 * de `AdminKpiController` mais filtré sur la boutique du vendeur connecté.
 */
public record SellerKpiResponse(
        BigDecimal revenueLast7Days,
        /** Variation vs les 7 jours précédents, en %, null si aucune vente sur cette période de référence. */
        Double revenueChangePercent,
        long ongoingOrdersCount,
        long readyToShipCount,
        BigDecimal availableBalance,
        List<SalesPointResponse> salesLast7Days,
        List<SalesPointResponse> salesLast14Days,
        List<SalesPointResponse> salesLast28Days,
        BestSellingProductResponse bestSellingProduct
) {
    public record SalesPointResponse(String date, BigDecimal amount) {}

    public record BestSellingProductResponse(String productName, long unitsSold) {}
}
