package com.maplenou.backend.catalog;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 230)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Montant en FCFA. Minimum 500 FCFA vérifié dans le service.
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    // Prix avant reduction. Null = pas de promo affichee (regle geree cote client).
    @Column(name = "old_price", precision = 12, scale = 2)
    private BigDecimal oldPrice;

    // Specifications libres (Marque, Matiere, Origine...), affichees telles quelles.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<ProductSpecification> specifications = new ArrayList<>();

    // Champs livraison par produit (optionnels) -- distincts du bareme de livraison par
    // palier de distance (DeliveryPricingTier), qui reste la source de verite pour le prix.
    @Column(name = "package_weight_kg", precision = 8, scale = 3)
    private BigDecimal packageWeightKg;

    @Column(name = "estimated_delivery_delay", length = 100)
    private String estimatedDeliveryDelay;

    @Column(name = "delivery_available", nullable = false)
    @Builder.Default
    private boolean deliveryAvailable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    // Soft delete : jamais de suppression physique d'un produit déjà commandé.
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

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
