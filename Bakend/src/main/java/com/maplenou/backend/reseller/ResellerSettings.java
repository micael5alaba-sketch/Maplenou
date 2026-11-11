package com.maplenou.backend.reseller;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Paramètres financiers du programme, VERSIONNÉS : une modification ajoute une ligne (l'ancienne
 * est conservée). Une commande garde la version appliquée à sa création ; les nouvelles règles ne
 * s'appliquent qu'aux nouvelles commandes.
 */
@Entity
@Table(name = "reseller_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private int version;

    /** Part du partenaire dans la commission Maplenou (0.25). */
    @Column(name = "share_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal shareRate;

    /** Prix unitaire final minimum pour être éligible (2 500 FCFA). */
    @Column(name = "min_unit_price", nullable = false)
    private long minUnitPrice;

    /** Seuil des paliers : jusqu'à ce prix inclus, taux bas ; au-dessus, taux haut (10 000 FCFA). */
    @Column(name = "tier_threshold", nullable = false)
    private long tierThreshold;

    @Column(name = "rate_low", nullable = false, precision = 5, scale = 4)
    private BigDecimal rateLow;

    @Column(name = "rate_high", nullable = false, precision = 5, scale = 4)
    private BigDecimal rateHigh;

    @Column(name = "attribution_days", nullable = false)
    private int attributionDays;

    @Column(name = "min_withdrawal", nullable = false)
    private long minWithdrawal;

    @Column(name = "withdrawal_fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal withdrawalFeeRate;

    @Column(name = "program_active", nullable = false)
    private boolean programActive;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
