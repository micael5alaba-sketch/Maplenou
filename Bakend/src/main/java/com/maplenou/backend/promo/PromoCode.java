package com.maplenou.backend.promo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    private PromoScopeType scopeType;

    /** NULL si scopeType = PLATFORM, sinon l'ID de la boutique. */
    @Column(name = "scope_id")
    private UUID scopeId;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    /** Montant minimum de commande pour appliquer le code (optionnel). */
    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    /** Nombre maximum d'utilisations totales. NULL = illimité. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private int currentUses = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

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

    // ── Logique métier ────────────────────────────────────────────────────────

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isExhausted() {
        return maxUses != null && currentUses >= maxUses;
    }

    /**
     * Vérifie si le code est applicable :
     * actif, non expiré, non épuisé, et montant minimum respecté.
     */
    public boolean isApplicableTo(BigDecimal orderAmount) {
        if (!active || isExpired() || isExhausted()) return false;
        return minOrderAmount == null || orderAmount.compareTo(minOrderAmount) >= 0;
    }

    /**
     * Calcule le montant de réduction sur un montant donné.
     * Ex: 10% sur 15 000 FCFA = 1 500 FCFA.
     */
    public BigDecimal computeDiscount(BigDecimal amount) {
        return amount.multiply(discountPercent)
                     .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}
