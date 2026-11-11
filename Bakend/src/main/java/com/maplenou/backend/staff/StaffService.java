package com.maplenou.backend.staff;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.auth.MfaService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.security.RefreshTokenSessionService;
import com.maplenou.backend.staff.dto.CreateStaffRequest;
import com.maplenou.backend.staff.dto.SetStaffActiveRequest;
import com.maplenou.backend.staff.dto.StaffCredentialsResponse;
import com.maplenou.backend.staff.dto.StaffResponse;
import com.maplenou.backend.staff.dto.UpdateStaffRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Comptes du personnel du back-office (document "Rôles et accès au tableau de bord", §9) : créés,
 * modifiés, activés, désactivés et réinitialisés UNIQUEMENT par le Super Admin (permission
 * STAFF_MANAGE), jamais par inscription publique. Chaque opération est tracée dans le journal d'audit.
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    // Sans caractères ambigus (0/O, 1/l/I) : le mot de passe initial est transmis à la main.
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final MfaService mfaService;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public Page<StaffResponse> list(Boolean active, Pageable pageable) {
        return userRepository.findStaff(active, pageable).map(StaffResponse::from);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(UUID id) {
        return StaffResponse.from(getStaffOrThrow(id));
    }

    @Transactional
    public StaffCredentialsResponse create(CreateStaffRequest request, User actor, String ip) {
        if (!request.role().isStaff()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce rôle n'est pas un rôle du personnel");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
        }

        String temporaryPassword = generateTemporaryPassword();
        User user = User.builder()
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(temporaryPassword))
                .role(request.role())
                .active(true)
                .phoneVerified(true)
                .mustChangePassword(true)
                .deliveryPricingGranted(request.role() == Role.DELIVERY_ADMIN
                        && Boolean.TRUE.equals(request.deliveryPricingGranted()))
                .build();
        userRepository.save(user);

        auditService.log(actor.getId(), AuditAction.STAFF_CREATED, "USER", user.getId(),
                "Compte du personnel créé | rôle : " + user.getRole()
                        + " | tarification livraison : " + user.isDeliveryPricingGranted(), ip);
        return new StaffCredentialsResponse(StaffResponse.from(user), temporaryPassword);
    }

    @Transactional
    public StaffResponse update(UUID id, UpdateStaffRequest request, User actor, String ip) {
        User user = getStaffOrThrow(id);

        if (request.role() != null && request.role() != user.getRole()) {
            if (!request.role().isStaff()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ce rôle n'est pas un rôle du personnel");
            }
            if (user.getId().equals(actor.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas modifier votre propre rôle");
            }
            ensureNotLastSuperAdmin(user);
            Role previous = user.getRole();
            user.setRole(request.role());
            if (request.role() != Role.DELIVERY_ADMIN) {
                user.setDeliveryPricingGranted(false);
            }
            // Les permissions sont recalculées à chaque requête ; on révoque en plus les sessions
            // de rafraîchissement pour qu'aucun jeton émis sous l'ancien rôle ne survive.
            refreshTokenSessionService.revokeAllForUser(user.getId(), "STAFF_ROLE_CHANGED");
            auditService.log(actor.getId(), AuditAction.STAFF_UPDATED, "USER", user.getId(),
                    "Rôle : " + previous + " → " + request.role(), ip);
        }
        if (request.deliveryPricingGranted() != null) {
            if (user.getRole() != Role.DELIVERY_ADMIN) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "La tarification de livraison ne s'attribue qu'à un Administrateur des livraisons");
            }
            user.setDeliveryPricingGranted(request.deliveryPricingGranted());
            auditService.log(actor.getId(), AuditAction.STAFF_UPDATED, "USER", user.getId(),
                    "Tarification de livraison attribuée : " + request.deliveryPricingGranted(), ip);
        }
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        return StaffResponse.from(userRepository.save(user));
    }

    /** Active ou désactive un compte. La désactivation est immédiate (jetons d'accès refusés, sessions révoquées). */
    @Transactional
    public StaffResponse setActive(UUID id, SetStaffActiveRequest request, User actor, String ip) {
        User user = getStaffOrThrow(id);
        boolean active = Boolean.TRUE.equals(request.active());

        if (!active) {
            if (user.getId().equals(actor.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Vous ne pouvez pas désactiver votre propre compte");
            }
            ensureNotLastSuperAdmin(user);
        }
        user.setActive(active);
        userRepository.save(user);
        if (!active) {
            refreshTokenSessionService.revokeAllForUser(user.getId(), "STAFF_DEACTIVATED");
        }
        auditService.log(actor.getId(), active ? AuditAction.STAFF_ACTIVATED : AuditAction.STAFF_DEACTIVATED,
                "USER", user.getId(),
                "Compte " + (active ? "activé" : "désactivé")
                        + (request.justification() != null ? " | motif : " + request.justification() : ""), ip);
        return StaffResponse.from(user);
    }

    /** Nouveau mot de passe initial + MFA à réinitialiser : l'utilisateur doit tout reconfigurer à la prochaine connexion. */
    @Transactional
    public StaffCredentialsResponse resetPassword(UUID id, User actor, String ip) {
        User user = getStaffOrThrow(id);
        if (user.getId().equals(actor.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Utilisez le changement de mot de passe de votre profil pour votre propre compte");
        }
        String temporaryPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);
        mfaService.deleteRecoveryCodes(user);
        refreshTokenSessionService.revokeAllForUser(user.getId(), "STAFF_PASSWORD_RESET");
        auditService.log(actor.getId(), AuditAction.STAFF_PASSWORD_RESET, "USER", user.getId(),
                "Mot de passe et MFA réinitialisés", ip);
        return new StaffCredentialsResponse(StaffResponse.from(user), temporaryPassword);
    }

    // ----- Privé -----

    private User getStaffOrThrow(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Compte introuvable"));
        if (user.getRole() == null || !user.getRole().isStaff()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Compte du personnel introuvable");
        }
        return user;
    }

    /** Garde-fou : il doit toujours rester au moins un Super Admin actif. */
    private void ensureNotLastSuperAdmin(User target) {
        if (target.getRole() == Role.ADMIN && target.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "Il doit rester au moins un Super Admin actif");
        }
    }

    /** 12 caractères, au moins une majuscule, une minuscule et un chiffre (politique de robustesse). */
    private String generateTemporaryPassword() {
        String all = UPPER + LOWER + DIGITS;
        char[] chars = new char[12];
        chars[0] = UPPER.charAt(random.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(random.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(random.nextInt(DIGITS.length()));
        for (int i = 3; i < chars.length; i++) {
            chars[i] = all.charAt(random.nextInt(all.length()));
        }
        for (int i = chars.length - 1; i > 0; i--) { // mélange
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}
