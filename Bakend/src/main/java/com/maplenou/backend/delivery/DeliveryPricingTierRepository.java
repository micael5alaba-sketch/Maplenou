package com.maplenou.backend.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DeliveryPricingTierRepository extends JpaRepository<DeliveryPricingTier, UUID> {

    /** Du palier le plus proche (petite distance) au palier illimité (NULL) en dernier. */
    @Query("SELECT t FROM DeliveryPricingTier t ORDER BY t.maxDistanceKm ASC NULLS LAST")
    List<DeliveryPricingTier> findAllOrderedByMaxDistanceKmAscNullsLast();

    boolean existsByMaxDistanceKmIsNullAndIdNot(UUID id);

    boolean existsByMaxDistanceKmIsNull();
}
