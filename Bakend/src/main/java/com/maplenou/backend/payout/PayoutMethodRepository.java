package com.maplenou.backend.payout;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayoutMethodRepository extends JpaRepository<PayoutMethod, UUID> {

    List<PayoutMethod> findByShopIdOrderByIsDefaultDescCreatedAtDesc(UUID shopId);

    Optional<PayoutMethod> findByIdAndShopId(UUID id, UUID shopId);

    boolean existsByShopId(UUID shopId);

    /** Retire le défaut de toutes les méthodes de la boutique avant d'en fixer une nouvelle. */
    @Modifying
    @Query("UPDATE PayoutMethod pm SET pm.isDefault = false WHERE pm.shop.id = :shopId")
    void clearDefaultForShop(@Param("shopId") UUID shopId);
}
