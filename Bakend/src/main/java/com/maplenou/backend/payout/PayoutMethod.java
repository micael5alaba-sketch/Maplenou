package com.maplenou.backend.payout;

import com.maplenou.backend.catalog.Shop;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Compte de destination enregistre par un vendeur pour recevoir ses versements
 * (T-Money, Flooz, virement bancaire). Purement declaratif cote plateforme :
 * aucune verification/validation du numero n'est faite pour l'instant, le
 * virement reste initie manuellement par l'admin (voir PayoutService).
 */
@Entity
@Table(name = "payout_methods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutMethodType type;

    // Numero de telephone (T-Money/Flooz) ou IBAN/RIB (virement).
    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
