package com.maplenou.backend.messaging;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    /** NULL pour une conversation SUPPORT. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    /** Client (BUYER_SELLER, SUPPORT, BUYER_COURIER). NULL pour SELLER_COURIER. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    /** Propriétaire de la boutique. NULL pour SUPPORT et BUYER_COURIER. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    /**
     * Livraison à laquelle la conversation est rattachée (BUYER_COURIER, SELLER_COURIER) — le
     * livreur participant se déduit de delivery.getAgent(), jamais stocké séparément. NULL pour
     * BUYER_SELLER et SUPPORT.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
