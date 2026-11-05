package com.maplenou.backend.courier;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Dernière position connue d'un livreur (une ligne par livreur, écrasée à chaque ping). */
@Entity
@Table(name = "courier_locations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourierLocation {

    @Id
    @Column(name = "agent_id")
    private UUID agentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", insertable = false, updatable = false)
    private User agent;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
