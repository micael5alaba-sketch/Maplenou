package com.maplenou.backend.revenue.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenus de la plateforme sur une période (dates UTC, bornes incluses). Trois sources :
 * commissions intégrées aux prix (sous-commandes livrées), part de la plateforme sur les frais de
 * livraison (25 % de chaque livraison terminée), et frais mensuels vendeurs réellement recouvrés.
 */
public record RevenueSummaryResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal commissions,
        BigDecimal deliveryShare,
        BigDecimal sellerFees,
        BigDecimal total,
        List<DailyRevenue> daily
) {
    public record DailyRevenue(LocalDate date, BigDecimal commissions, BigDecimal deliveryShare, BigDecimal sellerFees) {}
}
