package com.maplenou.backend.reseller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.reseller.dto.ResellerDtos.*;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Profil partenaire : demande, décision administrative (approbation, refus, suspension,
 * réactivation, désactivation). Toute décision est tracée avec son auteur, sa date et son motif.
 */
@Service
@RequiredArgsConstructor
public class ResellerService {

    /** Version des conditions du programme acceptées à la demande (conservée avec la date). */
    public static final String TERMS_VERSION = "2.0";

    private final ResellerProfileRepository profileRepository;
    private final ResellerTermsAcceptanceRepository termsRepository;
    private final ResellerAuditLogRepository auditRepository;
    private final ResellerCommissionRepository commissionRepository;
    private final ResellerClickRepository clickRepository;
    private final ResellerSettingsService settingsService;
    private final AuditService auditService;

    // ----- Partenaire -----

    @Transactional
    public ProfileResponse apply(User user, String ip) {
        if (user.getRole() != null && user.getRole() != Role.DELIVERY_AGENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Un compte du personnel ne peut pas devenir partenaire");
        }
        if (!settingsService.current().isProgramActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "Le programme partenaire est temporairement fermé");
        }
        ResellerProfile existing = profileRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null && existing.getStatus() != ResellerStatus.REJECTED) {
            throw new ApiException(HttpStatus.CONFLICT, "Une demande existe déjà pour ce compte (statut : "
                    + existing.getStatus() + ")");
        }
        Instant now = Instant.now();
        ResellerProfile profile = existing != null ? existing : ResellerProfile.builder().user(user).build();
        profile.setFullName(user.getFullName());
        profile.setPhoneNumber(user.getPhoneNumber());
        profile.setEmail(user.getEmail() != null ? user.getEmail() : "");
        profile.setStatus(ResellerStatus.PENDING);
        profile.setTermsVersion(TERMS_VERSION);
        profile.setTermsAcceptedAt(now);
        profile.setStatusReason(null);
        profile.setDecidedAt(null);
        profile.setDecidedBy(null);
        profile = profileRepository.save(profile);
        termsRepository.save(ResellerTermsAcceptance.builder()
                .userId(user.getId()).termsVersion(TERMS_VERSION).acceptedAt(now).ipAddress(ip).build());
        return toResponse(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse me(User user) {
        return toResponse(profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vous n'avez pas de demande partenaire")));
    }

    /** Profil du partenaire, quel que soit son statut (consultation de ses propres données). */
    @Transactional(readOnly = true)
    ResellerProfile requireProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas partenaire"));
    }

    /** Profil du partenaire ACTIF exigé pour les actions partenaire (liens, retraits...). */
    @Transactional(readOnly = true)
    ResellerProfile requireActive(User user) {
        ResellerProfile p = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Vous n'êtes pas partenaire"));
        if (!p.isActivePartner()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Votre compte partenaire n'est pas actif (statut : "
                    + p.getStatus() + ")");
        }
        return p;
    }

    // ----- Administration -----

    @Transactional(readOnly = true)
    public Page<ProfileResponse> list(ResellerStatus status, Pageable pageable) {
        return profileRepository.findByOptionalStatus(status, pageable).map(ResellerService::toResponse);
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional
    public ProfileResponse approve(UUID id, User actor, String ip) {
        ResellerProfile p = load(id);
        requireStatus(p, ResellerStatus.PENDING);
        return decide(p, ResellerStatus.APPROVED, null, actor, ip, AuditAction.RESELLER_APPROVED,
                "Demande partenaire approuvée", "Bienvenue dans le programme partenaire Maplenou. "
                        + "Vous pouvez maintenant générer vos liens.");
    }

    @Transactional
    public ProfileResponse reject(UUID id, String reason, User actor, String ip) {
        ResellerProfile p = load(id);
        requireStatus(p, ResellerStatus.PENDING);
        return decide(p, ResellerStatus.REJECTED, requireReason(reason), actor, ip, AuditAction.RESELLER_REJECTED,
                "Demande partenaire refusée", "Votre demande n'a pas été retenue : " + reason.trim());
    }

    @Transactional
    public ProfileResponse suspend(UUID id, String reason, User actor, String ip) {
        ResellerProfile p = load(id);
        requireStatus(p, ResellerStatus.APPROVED);
        return decide(p, ResellerStatus.SUSPENDED, requireReason(reason), actor, ip, AuditAction.RESELLER_SUSPENDED,
                "Compte partenaire suspendu", "Votre compte partenaire est suspendu : " + reason.trim());
    }

    @Transactional
    public ProfileResponse reactivate(UUID id, String reason, User actor, String ip) {
        ResellerProfile p = load(id);
        requireStatus(p, ResellerStatus.SUSPENDED, ResellerStatus.DEACTIVATED);
        return decide(p, ResellerStatus.APPROVED, requireReason(reason), actor, ip, AuditAction.RESELLER_REACTIVATED,
                "Compte partenaire réactivé", "Votre compte partenaire est de nouveau actif.");
    }

    @Transactional
    public ProfileResponse deactivate(UUID id, String reason, User actor, String ip) {
        ResellerProfile p = load(id);
        requireStatus(p, ResellerStatus.APPROVED, ResellerStatus.SUSPENDED);
        return decide(p, ResellerStatus.DEACTIVATED, requireReason(reason), actor, ip, AuditAction.RESELLER_SUSPENDED,
                "Compte partenaire désactivé", "Votre compte partenaire a été désactivé : " + reason.trim());
    }

    @Transactional(readOnly = true)
    public AdminStats stats() {
        return new AdminStats(
                profileRepository.countByStatus(ResellerStatus.PENDING),
                profileRepository.countByStatus(ResellerStatus.APPROVED),
                commissionRepository.sumShareByStatus(ResellerCommissionStatus.PENDING),
                commissionRepository.sumShareByStatus(ResellerCommissionStatus.CREDITED),
                0L);
    }

    @Transactional(readOnly = true)
    public Page<AuditEntry> audit(Pageable pageable) {
        return auditRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(a -> new AuditEntry(a.getId(), a.getActorId(), a.getAction(), a.getEntityType(),
                        a.getEntityId(), a.getOldValue(), a.getNewValue(), a.getReason(), a.getCreatedAt()));
    }

    // ----- Privé -----

    private ProfileResponse decide(ResellerProfile p, ResellerStatus to, String reason, User actor, String ip,
                                   AuditAction action, String title, String body) {
        ResellerStatus from = p.getStatus();
        p.setStatus(to);
        p.setStatusReason(reason != null ? reason.trim() : null);
        p.setDecidedBy(actor.getId());
        p.setDecidedAt(Instant.now());
        profileRepository.save(p);
        auditRepository.save(ResellerAuditLog.builder()
                .actorId(actor.getId()).action(to.name()).entityType("RESELLER_PROFILE").entityId(p.getId())
                .oldValue(from.name()).newValue(to.name()).reason(reason).build());
        auditService.log(actor.getId(), action, "RESELLER_PROFILE", p.getId(),
                from + " → " + to + (reason != null ? " | " + reason.trim() : ""), ip);
        return toResponse(p);
    }

    private ResellerProfile load(UUID id) {
        return profileRepository.findByIdWithUser(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
    }

    private static void requireStatus(ResellerProfile p, ResellerStatus... allowed) {
        for (ResellerStatus s : allowed) {
            if (p.getStatus() == s) return;
        }
        throw new ApiException(HttpStatus.CONFLICT, "Action impossible pour un partenaire au statut " + p.getStatus());
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un motif est obligatoire pour cette action");
        }
        return reason;
    }

    static ProfileResponse toResponse(ResellerProfile p) {
        return new ProfileResponse(p.getId(), p.getUser().getId(), p.getFullName(), p.getPhoneNumber(), p.getEmail(),
                p.getStatus(), p.getTermsVersion(), p.getTermsAcceptedAt(), p.getDecidedAt(), p.getStatusReason(),
                p.getCreatedAt());
    }
}
