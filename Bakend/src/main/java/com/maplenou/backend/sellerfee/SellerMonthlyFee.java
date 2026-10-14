package com.maplenou.backend.sellerfee;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.seller.SellerFeeCategory;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un frais mensuel d'activité pour une boutique — au plus un par (boutique, mois de
 * facturation), voir la contrainte unique en base (migration V48). Créé uniquement à partir de
 * la 4ᵉ vente éligible à vie du vendeur (voir {@link SellerFeeService#onSubOrderDelivered}),
 * jamais pour un mois sans vente éligible.
 *
 * {@code category}/{@code amount} sont figés au moment de la création (photo de la catégorie
 * active ce jour-là) — un changement de catégorie plus tard ne modifie jamais un frais déjà créé,
 * conformément à la spec ("conserver l'historique").
 */
@Entity
@Table(name = "seller_monthly_fees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerMonthlyFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /** Toujours le 1er jour du mois concerné. */
    @Column(name = "billing_month", nullable = false)
    private LocalDate billingMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellerFeeCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SellerMonthlyFeeStatus status = SellerMonthlyFeeStatus.PENDING;

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

    public BigDecimal remaining() {
        return amount.subtract(amountPaid);
    }
}
