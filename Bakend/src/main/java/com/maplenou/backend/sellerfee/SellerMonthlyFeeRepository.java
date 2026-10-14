package com.maplenou.backend.sellerfee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerMonthlyFeeRepository extends JpaRepository<SellerMonthlyFee, UUID> {

    Optional<SellerMonthlyFee> findByShopIdAndBillingMonth(UUID shopId, LocalDate billingMonth);

    /** Frais encore dus (pas totalement payés ni annulés), du plus ancien au plus récent. */
    List<SellerMonthlyFee> findByShopIdAndStatusInOrderByBillingMonthAsc(
            UUID shopId, List<SellerMonthlyFeeStatus> statuses);

    Page<SellerMonthlyFee> findByShopIdOrderByBillingMonthDesc(UUID shopId, Pageable pageable);
}
