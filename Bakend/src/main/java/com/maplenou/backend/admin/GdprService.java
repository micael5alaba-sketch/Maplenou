package com.maplenou.backend.admin;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.DeviceTokenRepository;
import com.maplenou.backend.user.AddressRepository;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GdprService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final AuditService auditService;

    @Transactional
    public void anonymizeUser(User actor, UUID targetUserId, String ipAddress) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        if ("[Supprimé]".equals(target.getFullName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cet utilisateur est déjà anonymisé");
        }

        if (target.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Impossible d'anonymiser un administrateur");
        }

        doAnonymize(target, targetUserId);

        auditService.log(
                actor.getId(),
                AuditAction.USER_ANONYMIZED,
                "USER",
                targetUserId,
                "User anonymized per GDPR request",
                ipAddress
        );
    }

    @Transactional
    public void selfDelete(User user) {
        if ("[Supprimé]".equals(user.getFullName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce compte est déjà anonymisé");
        }

        doAnonymize(user, user.getId());

        auditService.log(
                user.getId(),
                AuditAction.USER_ANONYMIZED,
                "USER",
                user.getId(),
                "User self-deleted per GDPR request",
                null
        );
    }

    private void doAnonymize(User user, UUID userId) {
        user.setFullName("[Supprimé]");
        user.setPhoneNumber("DELETED-" + userId.toString().substring(0, 8));
        user.setEmail(null);
        user.setActive(false);
        user.setTotpEnabled(false);
        user.setTotpSecret(null);

        deviceTokenRepository.deleteAllByUserId(userId);
        addressRepository.deleteAllByUserId(userId);

        userRepository.save(user);
    }
}
