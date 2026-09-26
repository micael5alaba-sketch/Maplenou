package com.maplenou.backend.delivery;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Gain d'un livreur pour une livraison terminée. Montant provisoire : les
 * frais de la {@link DeliveryZone} correspondant à l'adresse de livraison
 * (voir {@link CourierEarningService}) — l'`Order` ne mémorise pas encore
 * quelle zone a été appliquée au moment du checkout, donc la zone est
 * déduite a posteriori par correspondance de ville/quartier.
 */
@Entity
@Table(name = "courier_earnings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierEarning {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourierEarningStatus status = CourierEarningStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

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
