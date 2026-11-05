package com.maplenou.backend.order;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.payout.Payout;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sub_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Taux de commission integre au prix affiche : 0.0500 (Standard) ou 0.0300 (Prix eleves).
     * Les commandes anterieures au 05/11/2026 portent l'ancien taux 0.2500 / 0.2250. */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    /** Ce que recoit le vendeur : somme des prix de base de ses produits (commission non preleve). */
    @Column(name = "net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    /** Categorie tarifaire du vendeur au moment de la commande (historique, jamais reecrit).
     * Null pour les commandes anterieures au modele de commission integree. */
    @Enumerated(EnumType.STRING)
    @Column(name = "fee_category", length = 20)
    private com.maplenou.backend.seller.SellerFeeCategory feeCategory;

    /** Frais de livraison réellement facturé pour cette sous-commande (tarif de base de la
     * zone + distance réelle boutique↔adresse × prix/km, voir DeliveryPricingService).
     * Figé au moment de la commande — jamais recalculé après coup. */
    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    /** Distance réelle (à vol d'oiseau) utilisée pour calculer {@link #deliveryFee} — null si
     * les coordonnées de la boutique ou de l'adresse manquaient (tarif de base seul appliqué). */
    @Column(name = "delivery_distance_km", precision = 8, scale = 2)
    private BigDecimal deliveryDistanceKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SubOrderStatus status = SubOrderStatus.PENDING;

    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubOrderItem> items = new ArrayList<>();

    /** Payout auquel ce sous-ordre est rattaché (null tant qu'il n'a pas été inclus dans un payout). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payout_id")
    private Payout payout;

    // ── Transporteur tiers (bordereau d'expédition, tracking) ──────────────────
    @Column(name = "carrier_code", length = 30)
    private String carrierCode;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "carrier_label_url", length = 500)
    private String carrierLabelUrl;

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
