package com.maplenou.backend.delivery;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Gain d'un livreur pour une livraison terminée. Le montant brut (frais de livraison réel
 * du sous-ordre, figé au moment de la commande par {@link DeliveryPricingService} selon le
 * palier de distance applicable — voir {@link CourierEarningService}) est toujours partagé
 * 25 % plateforme / 75 % livreur ; {@code amount} reste le montant net déjà versable au livreur.
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

    /** Tarif complet de la zone de livraison, avant répartition. */
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    /** Part gardée par la plateforme (0.2500 = 25 %). */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /** Montant gardé par la plateforme, {@code grossAmount * commissionRate}. */
    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    /** Montant net versable au livreur, {@code grossAmount - commissionAmount}. */
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
