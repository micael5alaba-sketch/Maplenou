package com.maplenou.backend.sellerfee;

import com.maplenou.backend.seller.SellerFeeCategory;
import com.maplenou.backend.seller.SellerProfile;
import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Demande de changement de catégorie tarifaire — toujours effective au 1er jour du mois suivant
 * son approbation, jamais rétroactivement (voir {@link SellerFeeService#applyDueCategoryChange}).
 */
@Entity
@Table(name = "seller_fee_category_change_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerFeeCategoryChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_category", nullable = false, length = 20)
    private SellerFeeCategory requestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SellerFeeCategoryChangeStatus status = SellerFeeCategoryChangeStatus.PENDING;

    /** 1er jour du mois suivant l'approbation — renseigné seulement une fois approuvé. */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    /** Vrai une fois que {@code SellerProfile.feeCategory} a effectivement été mis à jour. */
    @Column(nullable = false)
    @Builder.Default
    private boolean applied = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
