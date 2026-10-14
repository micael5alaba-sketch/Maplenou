package com.maplenou.backend.sellerfee;

import com.maplenou.backend.payout.Payout;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Une déduction réelle appliquée à un {@link SellerMonthlyFee}, au moment d'un reversement
 * vendeur (voir {@code PayoutService#generatePayoutsForPeriod}). Trace complète pour
 * l'historique vendeur ("Historique : Déductions, paiements partiels, références de
 * transaction et dates").
 */
@Entity
@Table(name = "seller_fee_deductions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerFeeDeduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monthly_fee_id", nullable = false)
    private SellerMonthlyFee monthlyFee;

    /** Le reversement qui a servi de source à cette déduction. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private Payout payout;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
