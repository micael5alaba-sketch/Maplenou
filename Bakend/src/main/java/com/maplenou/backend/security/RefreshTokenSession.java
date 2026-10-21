package com.maplenou.backend.security;

import com.maplenou.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace chaque refresh token émis (par son hash, jamais la valeur brute) pour appliquer une
 * rotation stricte à usage unique : {@link AuthService#refresh} marque le jeton présenté
 * ROTATED et insère son successeur dans la même {@code familyId}. Si un jeton déjà ROTATED est
 * représenté (rejeu d'un jeton remplacé), toute la famille est révoquée — c'est la défense
 * centrale du correctif F-02 (pentest du 21/10/2026) : avant ce mécanisme, un refresh token
 * restait utilisable indéfiniment même après avoir produit son successeur.
 */
@Entity
@Table(name = "refresh_token_session")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Relie tous les jetons successifs d'une même session de connexion. */
    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefreshTokenStatus status;

    /** Jeton dont celui-ci est le successeur (trace de rotation), null pour le premier de la famille. */
    @Column(name = "parent_id")
    private UUID parentId;

    /** Hash de l'IP d'émission — signal d'audit seulement, jamais une preuve d'identité à elle seule. */
    @Column(name = "ip_hash", length = 128)
    private String ipHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;
}
