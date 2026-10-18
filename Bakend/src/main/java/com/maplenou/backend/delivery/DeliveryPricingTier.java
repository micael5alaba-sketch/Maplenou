package com.maplenou.backend.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Un palier du barème de livraison par distance (décision porteur de projet, voir
 * {@link DeliveryPricingService}) : "jusqu'à {@code maxDistanceKm} km, le client paie
 * {@code price}". {@code maxDistanceKm} nul = dernier palier, "au-delà de X km" (illimité) —
 * au plus un palier illimité à la fois (contrainte en base, voir V50).
 */
@Entity
@Table(name = "delivery_pricing_tiers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "max_distance_km", precision = 6, scale = 2)
    private BigDecimal maxDistanceKm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
