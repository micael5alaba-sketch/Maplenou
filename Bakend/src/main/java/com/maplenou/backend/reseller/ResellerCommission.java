package com.maplenou.backend.reseller;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Commission d'UNE unité vendue grâce au lien d'un partenaire, calculée à la création de la commande
 * (prix, taux, part partenaire et attribution figés) et créditée seulement à la validation de la
 * livraison. Unicité (ligne de commande, unité) : jamais deux commissions pour la même unité.
 */
@Entity
@Table(name = "reseller_commissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"order_item_id", "unit_index"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "unit_index", nullable = false)
    private short unitIndex;

    @Column(name = "attribution_id", nullable = false)
    private UUID attributionId;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    /** Prix unitaire final réellement payé (après réduction), en FCFA entiers. */
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "maplenou_commission", nullable = false)
    private long maplenouCommission;

    @Column(name = "partner_share", nullable = false)
    private long partnerShare;

    @Column(name = "maplenou_share", nullable = false)
    private long maplenouShare;

    @Column(name = "settings_version", nullable = false)
    private int settingsVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResellerCommissionStatus status = ResellerCommissionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "credited_at")
    private Instant creditedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
