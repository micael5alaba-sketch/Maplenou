package com.maplenou.backend.order;

import com.maplenou.backend.catalog.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sub_order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrder subOrder;

    /** Nullable : la variante peut être supprimée (soft-delete) après la commande. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    // Snapshot au moment de la commande — ne varie plus jamais
    @Column(name = "snapshot_product_name", nullable = false, length = 255)
    private String snapshotProductName;

    @Column(name = "snapshot_variant_label", nullable = false, length = 100)
    private String snapshotVariantLabel;

    @Column(name = "snapshot_sku", nullable = false, length = 100)
    private String snapshotSku;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private short quantity;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
