package com.maplenou.backend.seller;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.catalog.ShopRepository;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.order.SubOrderItemRepository;
import com.maplenou.backend.order.SubOrderRepository;
import com.maplenou.backend.order.SubOrderStatus;
import com.maplenou.backend.seller.dto.SellerKpiResponse;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Agrégats du dashboard vendeur — équivalent d'`AdminKpiService`, filtré
 * sur la boutique du vendeur connecté (voir `DEMANDES_MODIFICATIONS_BACKEND.md`
 * §11 : gap identifié, comblé ici).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerKpiService {

    private final ShopRepository shopRepository;
    private final SubOrderRepository subOrderRepository;
    private final SubOrderItemRepository subOrderItemRepository;

    public SellerKpiResponse getDashboard(User currentUser) {
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucune boutique pour ce compte"));

        Instant now = Instant.now();
        List<SubOrder> last28Days = subOrderRepository.findByShopIdSince(shop.getId(), now.minus(28, ChronoUnit.DAYS));

        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant fourteenDaysAgo = now.minus(14, ChronoUnit.DAYS);

        BigDecimal revenueLast7Days = sumSubtotal(last28Days, sevenDaysAgo, now);
        BigDecimal revenuePrevious7Days = sumSubtotal(last28Days, fourteenDaysAgo, sevenDaysAgo);
        Double revenueChangePercent = revenuePrevious7Days.signum() == 0
                ? null
                : revenueLast7Days.subtract(revenuePrevious7Days)
                        .divide(revenuePrevious7Days, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        long ongoing = subOrderRepository.countOngoingByShopId(shop.getId());
        long readyToShip = subOrderRepository.countByShopIdAndStatus(shop.getId(), SubOrderStatus.READY_FOR_PICKUP);
        BigDecimal availableBalance = subOrderRepository.sumAvailableBalanceByShopId(shop.getId());

        List<SubOrderItemRepository.BestSellingRow> bestSellingRows =
                subOrderItemRepository.findBestSellingByShopId(shop.getId(), PageRequest.of(0, 1));
        SellerKpiResponse.BestSellingProductResponse bestSelling = bestSellingRows.isEmpty() ? null
                : new SellerKpiResponse.BestSellingProductResponse(
                        bestSellingRows.get(0).getProductName(), bestSellingRows.get(0).getTotalQuantity());

        return new SellerKpiResponse(
                revenueLast7Days,
                revenueChangePercent,
                ongoing,
                readyToShip,
                availableBalance,
                buildDailySeries(last28Days, now, 7),
                buildDailySeries(last28Days, now, 14),
                buildDailySeries(last28Days, now, 28),
                bestSelling
        );
    }

    private BigDecimal sumSubtotal(List<SubOrder> subOrders, Instant from, Instant to) {
        return subOrders.stream()
                .filter(so -> !so.getCreatedAt().isBefore(from) && so.getCreatedAt().isBefore(to))
                .map(SubOrder::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<SellerKpiResponse.SalesPointResponse> buildDailySeries(List<SubOrder> subOrders, Instant now, int days) {
        Instant since = now.minus(days, ChronoUnit.DAYS);
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        for (int i = 0; i < days; i++) {
            byDay.put(today.minusDays((long) days - 1 - i), BigDecimal.ZERO);
        }
        for (SubOrder so : subOrders) {
            if (so.getCreatedAt().isBefore(since)) continue;
            LocalDate day = LocalDate.ofInstant(so.getCreatedAt(), ZoneOffset.UTC);
            byDay.merge(day, so.getSubtotal(), BigDecimal::add);
        }
        return byDay.entrySet().stream()
                .map(e -> new SellerKpiResponse.SalesPointResponse(e.getKey().toString(), e.getValue()))
                .toList();
    }
}
