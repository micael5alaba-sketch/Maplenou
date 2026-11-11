package com.maplenou.backend.delivery;

import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Retrait des gains d'un livreur : regroupe les gains ({@link CourierEarning}) non encore versés
 * au moment de la demande, et suit le même circuit obligatoire que les retraits vendeurs
 * (réception → vérification → soumission → décision du Super Admin avec MFA → paiement).
 */
@Entity
@Table(name = "courier_withdrawals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    /** Somme des gains nets inclus dans ce retrait. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PayoutStatus status = PayoutStatus.PENDING;

    /** Coordonnées de paiement indiquées par le livreur (ex. "T-Money +22890123456"). */
    @Column(name = "payment_details", nullable = false, length = 100)
    private String paymentDetails;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    /** Observations et justifications successives des agents et du Super Admin. */
    @Column(columnDefinition = "TEXT")
    private String notes;

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
