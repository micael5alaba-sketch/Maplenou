package com.maplenou.backend.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_zones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ex: "Grand Lomé", "Baguida", "Avépozo", "Kpogan" */
    @Column(nullable = false, length = 150, unique = true)
    private String name;

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee;

    /** Délai estimé en minutes */
    @Column(name = "estimated_time_minutes", nullable = false)
    @Builder.Default
    private int estimatedTimeMinutes = 60;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

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
