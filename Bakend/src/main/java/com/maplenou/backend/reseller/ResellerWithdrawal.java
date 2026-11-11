package com.maplenou.backend.reseller;

import com.maplenou.backend.payout.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Demande de retrait d'un partenaire. Le montant est RÉSERVÉ dès la demande (écriture au registre),
 * libéré en cas de rejet, et les frais de 3 % ne sont prélevés qu'au paiement effectif. Suit le même
 * circuit de vérification et d'approbation que les autres retraits (voir WithdrawalWorkflow).
 */
@Entity
@Table(name = "reseller_withdrawals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private ResellerProfile partner;

    /** Montant demandé (FCFA entiers). */
    @Column(nullable = false)
    private long amount;

    @Column(name = "fee_amount", nullable = false)
    @Builder.Default
    private long feeAmount = 0;

    @Column(name = "net_amount", nullable = false)
    @Builder.Default
    private long netAmount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ResellerPaymentMethod method;

    @Column(name = "payment_phone", nullable = false, length = 20)
    private String paymentPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
