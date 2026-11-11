package com.maplenou.backend.reseller;

import com.maplenou.backend.catalog.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Lien personnel d'un partenaire pour un produit (un seul par couple partenaire/produit). */
@Entity
@Table(name = "reseller_referral_links")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResellerReferralLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private ResellerProfile partner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
