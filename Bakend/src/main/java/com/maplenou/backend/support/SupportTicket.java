package com.maplenou.backend.support;

import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.messaging.Conversation;
import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Ticket de support structuré (livreur ou acheteur) : numéro de suivi réel (généré en base via
 * {@code support_ticket_seq}, voir {@link SupportTicketService#generateTicketNumber}), catégorie,
 * commande/livraison liée optionnelle, photo de preuve optionnelle, et statut suivi par l'admin.
 * Le message initial est aussi envoyé dans la conversation support classique
 * ({@link #conversation}) pour que les réponses passent par la messagerie déjà existante.
 */
@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, length = 30, unique = true)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Optionnel : la livraison concernée, côté livreur (voir cahier des charges §3.3). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_delivery_id")
    private Delivery relatedDelivery;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    /** Conversation support où le premier message a été envoyé — pour continuer l'échange. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

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
