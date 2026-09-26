package com.maplenou.backend.courier;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Document légal soumis par un livreur (permis, pièce d'identité, assurance véhicule, carte
 * grise) — un seul par (courier, type) : un nouvel envoi remplace le précédent et repasse en
 * PENDING (voir {@link CourierDocumentService#submit}), il n'y a pas d'historique des anciens
 * fichiers remplacés.
 */
@Entity
@Table(name = "courier_documents", uniqueConstraints = @UniqueConstraint(columnNames = {"courier_id", "type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "courier_id", nullable = false)
    private User courier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourierDocumentType type;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CourierDocumentStatus status = CourierDocumentStatus.PENDING;

    /** Optionnelle : la pièce d'identité n'a pas forcément de date d'expiration renseignée. */
    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

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

    /** EXPIRED prime sur le statut stocké — jamais persisté directement, toujours recalculé. */
    public CourierDocumentStatus effectiveStatus() {
        if (status == CourierDocumentStatus.VALID && expiresAt != null && expiresAt.isBefore(LocalDate.now())) {
            return CourierDocumentStatus.EXPIRED;
        }
        return status;
    }
}
