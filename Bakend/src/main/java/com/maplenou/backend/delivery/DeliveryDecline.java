package com.maplenou.backend.delivery;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Refus d'une course par un livreur : la livraison ne lui est plus reproposée. */
@Entity
@Table(name = "delivery_declines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDecline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @Column(name = "declined_at", nullable = false)
    private Instant declinedAt;

    @PrePersist
    void onCreate() {
        if (declinedAt == null) declinedAt = Instant.now();
    }
}
