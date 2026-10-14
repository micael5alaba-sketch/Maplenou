package com.maplenou.backend.sellerfee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SellerFeeDeductionRepository extends JpaRepository<SellerFeeDeduction, UUID> {

    @EntityGraph(attributePaths = {"monthlyFee", "payout"})
    @Query("SELECT d FROM SellerFeeDeduction d WHERE d.monthlyFee.shop.id = :shopId ORDER BY d.createdAt DESC")
    Page<SellerFeeDeduction> findByShopId(@Param("shopId") UUID shopId, Pageable pageable);
}
