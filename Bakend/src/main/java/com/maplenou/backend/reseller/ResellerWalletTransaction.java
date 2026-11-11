package com.maplenou.backend.reseller;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Écriture du registre financier du portefeuille. IMMUABLE : un trigger PostgreSQL interdit toute
 * modification ou suppression ; une erreur se corrige par une écriture de régularisation (ADJUSTMENT)
 * associée à son motif et à l'administrateur responsable. Les soldes se calculent uniquement par
 * somme de {@code availableDelta} / {@code reservedDelta}.
 */
@Entity
@Table(name = "reseller_wallet_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "partner_id", nullable = false, updatable = false)
    private UUID partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private WalletTxType type;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(name = "available_delta", nullable = false, updatable = false)
    private long availableDelta;

    @Column(name = "reserved_delta", nullable = false, updatable = false)
    private long reservedDelta;

    @Column(name = "fee_amount", nullable = false, updatable = false)
    @Builder.Default
    private long feeAmount = 0;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(length = 100, updatable = false)
    private String reference;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String reason;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    /** Clé d'idempotence : un même événement (ex. livraison validée deux fois) ne s'écrit qu'une fois. */
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120, updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
