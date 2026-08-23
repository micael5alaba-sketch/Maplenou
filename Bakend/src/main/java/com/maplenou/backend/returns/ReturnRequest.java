package com.maplenou.backend.returns;

import com.maplenou.backend.order.SubOrder;
import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Un seul retour possible par sous-commande. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_order_id", nullable = false, unique = true)
    private SubOrder subOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    /** Date de livraison confirmée (snapshot de Delivery.deliveredAt). */
    @Column(name = "delivery_date", nullable = false)
    private Instant deliveryDate;

    /** Délai limite = deliveryDate + 30 jours. Calculé à la création. */
    @Column(name = "return_deadline", nullable = false)
    private Instant returnDeadline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition", nullable = false, length = 20)
    private ProductCondition productCondition;

    /** Null tant que pas décidé. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReturnDecision decision;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Vendeur ou admin qui a pris la décision. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", nullable = false, length = 20)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.PENDING;

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
