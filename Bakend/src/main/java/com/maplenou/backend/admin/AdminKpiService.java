package com.maplenou.backend.admin;

import com.maplenou.backend.admin.dto.DashboardKpiResponse;
import com.maplenou.backend.admin.dto.KpiPeriod;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.catalog.ShopStatus;
import com.maplenou.backend.delivery.DeliveryRepository;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.order.OrderRepository;
import com.maplenou.backend.order.OrderStatus;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminKpiService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final DeliveryRepository deliveryRepository;
    private final SubOrderRepository subOrderRepository;

    public DashboardKpiResponse getDashboard(KpiPeriod period) {
        Instant periodStart = resolvePeriodStart(period);

        // Revenue
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        BigDecimal totalCommission = subOrderRepository.sumTotalCommission();
        BigDecimal periodRevenue = periodStart != null
                ? orderRepository.sumRevenueSince(periodStart)
                : totalRevenue;

        // Orders
        long totalOrders = orderRepository.count();
        long paidOrders = orderRepository.countByStatus(OrderStatus.PAID);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);
        long periodNewOrders = periodStart != null
                ? orderRepository.countCreatedSince(periodStart)
                : totalOrders;

        // Users
        long totalUsers = userRepository.count();
        long periodNewUsers = periodStart != null
                ? userRepository.countCreatedSince(periodStart)
                : totalUsers;

        // Shops
        long totalShops = shopRepository.count();
        long pendingShops = shopRepository.countByStatus(ShopStatus.PENDING);
        long approvedShops = shopRepository.countByStatus(ShopStatus.APPROVED);
        long suspendedShops = shopRepository.countByStatus(ShopStatus.SUSPENDED);

        // Deliveries
        long totalDeliveries = deliveryRepository.count();
        long pendingDeliveries = deliveryRepository.countByStatus(DeliveryStatus.PENDING);
        long inTransitDeliveries = deliveryRepository.countByStatus(DeliveryStatus.IN_TRANSIT);
        long deliveredDeliveries = deliveryRepository.countByStatus(DeliveryStatus.DELIVERED);
        long failedDeliveries = deliveryRepository.countByStatus(DeliveryStatus.FAILED);

        return new DashboardKpiResponse(
                new DashboardKpiResponse.RevenueKpi(totalRevenue, totalCommission, periodRevenue),
                new DashboardKpiResponse.OrderKpi(totalOrders, paidOrders, cancelledOrders, periodNewOrders),
                new DashboardKpiResponse.UserKpi(totalUsers, periodNewUsers),
                new DashboardKpiResponse.ShopKpi(totalShops, pendingShops, approvedShops, suspendedShops),
                new DashboardKpiResponse.DeliveryKpi(totalDeliveries, pendingDeliveries, inTransitDeliveries, deliveredDeliveries, failedDeliveries)
        );
    }

    private Instant resolvePeriodStart(KpiPeriod period) {
        if (period == null || period == KpiPeriod.ALL_TIME) {
            return null;
        }
        return switch (period) {
            case TODAY -> Instant.now().truncatedTo(ChronoUnit.DAYS);
            case LAST_7_DAYS -> Instant.now().minus(7, ChronoUnit.DAYS);
            case LAST_30_DAYS -> Instant.now().minus(30, ChronoUnit.DAYS);
            case ALL_TIME -> null;
        };
    }
}
