package com.maplenou.backend.security;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.Sha256Utils;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Rotation stricte des refresh tokens (correctif F-02, pentest du 21/10/2026) : chaque jeton
 * n'est utilisable qu'une fois pour produire son successeur. Un jeton déjà consommé présenté à
 * nouveau (rejeu — vol probable) révoque immédiatement toute sa famille de session.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenSessionService {

    private final RefreshTokenSessionRepository repository;
    private final JwtService jwtService;
    private final AuditService auditService;

    /** Premier jeton d'une nouvelle session (register/login/2FA) : amorce une nouvelle famille. */
    @Transactional
    public void createInitialSession(User user, String refreshToken, String ipHash) {
        RefreshTokenSession session = RefreshTokenSession.builder()
                .familyId(UUID.randomUUID())
                .user(user)
                .tokenHash(Sha256Utils.hex(refreshToken))
                .status(RefreshTokenStatus.ACTIVE)
                .ipHash(ipHash)
                .issuedAt(Instant.now())
                .expiresAt(jwtService.extractExpiration(refreshToken).toInstant())
                .build();
        repository.save(session);
    }

    /**
     * Fait tourner un refresh token : le jeton présenté doit être ACTIVE, devient ROTATED, et son
     * successeur est inséré dans la même famille. Si le jeton présenté est introuvable en base,
     * il est traité comme émis avant le déploiement de ce mécanisme (voir migration V52) : accepté
     * une seule fois pour amorcer une nouvelle famille, plutôt que de déconnecter en masse tous
     * les utilisateurs déjà connectés au moment du déploiement — ce trou se résorbe naturellement
     * en 7 jours (durée de vie d'un refresh token, voir app.jwt.refresh-token-expiration-ms).
     */
    /**
     * noRollbackFor : cette méthode lève volontairement une ApiException (401) après avoir écrit
     * la révocation de la famille en base dans le cas d'un rejeu — sans cette annotation, le
     * rollback par défaut de Spring sur une RuntimeException annulerait cette révocation, la
     * laissant ACTIVE malgré l'alerte, et un jeton de la famille resterait utilisable (trouvé en
     * test : refreshTokenReuseAfterRotationRevokesWholeFamily échouait pour exactement cette raison).
     */
    @Transactional(noRollbackFor = ApiException.class)
    public void rotate(User user, String presentedRefreshToken, String newRefreshToken, String ipHash) {
        String presentedHash = Sha256Utils.hex(presentedRefreshToken);
        int rotated = repository.markRotatedIfActive(presentedHash);
        RefreshTokenSession presented = repository.findByTokenHash(presentedHash).orElse(null);

        if (rotated == 0) {
            if (presented != null && presented.getStatus() == RefreshTokenStatus.ROTATED) {
                int revokedCount = repository.revokeFamily(presented.getFamilyId(), Instant.now(), "REUSE_DETECTED");
                log.warn("REFRESH_REUSE_DETECTED familyId={} userId={} revokedSessions={}",
                        presented.getFamilyId(), user.getId(), revokedCount);
                auditService.log(user.getId(), AuditAction.REFRESH_TOKEN_REUSE_DETECTED, "USER", user.getId(),
                        "Rejeu d'un refresh token déjà utilisé détecté — famille de session révoquée ("
                                + revokedCount + " jetons)", null);
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide, veuillez vous reconnecter");
            }
            if (presented != null) {
                // REVOKED (logout, reuse déjà traité, ou changement de mot de passe) : 401 générique.
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement invalide");
            }
            // Introuvable en base : jeton pré-existant au déploiement — accepté une fois, nouvelle famille.
            UUID legacyFamilyId = UUID.randomUUID();
            repository.save(RefreshTokenSession.builder()
                    .familyId(legacyFamilyId)
                    .user(user)
                    .tokenHash(presentedHash)
                    .status(RefreshTokenStatus.ROTATED)
                    .ipHash(ipHash)
                    .issuedAt(Instant.now())
                    .expiresAt(jwtService.extractExpiration(presentedRefreshToken).toInstant())
                    .build());
            insertSuccessor(legacyFamilyId, null, user, newRefreshToken, ipHash);
            return;
        }

        insertSuccessor(presented.getFamilyId(), presented.getId(), user, newRefreshToken, ipHash);
    }

    private void insertSuccessor(UUID familyId, UUID parentId, User user, String newRefreshToken, String ipHash) {
        repository.save(RefreshTokenSession.builder()
                .familyId(familyId)
                .user(user)
                .tokenHash(Sha256Utils.hex(newRefreshToken))
                .status(RefreshTokenStatus.ACTIVE)
                .parentId(parentId)
                .ipHash(ipHash)
                .issuedAt(Instant.now())
                .expiresAt(jwtService.extractExpiration(newRefreshToken).toInstant())
                .build());
    }

    /** Déconnexion : révoque toute la famille du jeton présenté, pas seulement ce jeton précis. */
    @Transactional
    public void revokeFamilyByToken(String refreshToken, String reason) {
        repository.findByTokenHash(Sha256Utils.hex(refreshToken))
                .ifPresent(session -> repository.revokeFamily(session.getFamilyId(), Instant.now(), reason));
    }

    /** Changement de mot de passe : révoque toutes les sessions actives de l'utilisateur. */
    @Transactional
    public void revokeAllForUser(UUID userId, String reason) {
        repository.revokeAllActiveForUser(userId, Instant.now(), reason);
    }
}
