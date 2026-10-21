package com.maplenou.backend.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, UUID> {

    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

    /**
     * Rotation atomique : ne marque ROTATED que si le jeton est encore ACTIVE. Exactement le même
     * principe que {@code ProductVariantRepository.decrementStock}/{@code DeliveryRepository.claim} —
     * une seule requête UPDATE...WHERE conditionnelle, jamais de lecture-puis-écriture séparée.
     * Si deux requêtes présentent le même jeton en même temps, une seule affecte une ligne ;
     * l'autre (0 ligne affectée) relit le statut et détecte un rejeu (voir RefreshTokenSessionService).
     */
    @Modifying
    @Query("UPDATE RefreshTokenSession r SET r.status = com.maplenou.backend.security.RefreshTokenStatus.ROTATED " +
           "WHERE r.tokenHash = :tokenHash AND r.status = com.maplenou.backend.security.RefreshTokenStatus.ACTIVE")
    int markRotatedIfActive(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenSession r SET r.status = com.maplenou.backend.security.RefreshTokenStatus.REVOKED, " +
           "r.revokedAt = :now, r.revokedReason = :reason " +
           "WHERE r.familyId = :familyId AND r.status <> com.maplenou.backend.security.RefreshTokenStatus.REVOKED")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE RefreshTokenSession r SET r.status = com.maplenou.backend.security.RefreshTokenStatus.REVOKED, " +
           "r.revokedAt = :now, r.revokedReason = :reason " +
           "WHERE r.user.id = :userId AND r.status = com.maplenou.backend.security.RefreshTokenStatus.ACTIVE")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);
}
