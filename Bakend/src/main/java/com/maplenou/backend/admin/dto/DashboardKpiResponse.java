package com.maplenou.backend.admin.dto;

import java.math.BigDecimal;

public record DashboardKpiResponse(
        RevenueKpi revenue,
        OrderKpi orders,
        UserKpi users,
        ShopKpi shops,
        DeliveryKpi deliveries
) {
    public record RevenueKpi(BigDecimal totalRevenue, BigDecimal totalCommission, BigDecimal periodRevenue) {}
    public record OrderKpi(long total, long paid, long cancelled, long periodNew) {}
    public record UserKpi(long total, long periodNew) {}
    public record ShopKpi(long total, long pending, long approved, long suspended) {}
    public record DeliveryKpi(long total, long pending, long inTransit, long delivered, long failed) {}
}
