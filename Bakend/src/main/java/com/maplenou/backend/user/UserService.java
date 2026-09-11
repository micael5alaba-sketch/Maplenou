package com.maplenou.backend.user;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.dto.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    public User getByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    @Transactional
    public User updateProfile(UUID id, UpdateProfileRequest request) {
        User user = getById(id);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName());
        }
        if (request.email() != null && !request.email().isBlank()) {
            user.setEmail(request.email());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().isBlank() ? null : request.avatarUrl());
        }
        return userRepository.save(user);
    }

    // ----- Admin -----

    @Transactional(readOnly = true)
    public Page<User> listUsers(Role role, Boolean active, Pageable pageable) {
        return userRepository.findByFilters(role, active, pageable);
    }

    @Transactional
    public User banUser(UUID targetId, UUID actorId, String ip) {
        User user = getById(targetId);
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Impossible de bannir un administrateur");
        }
        user.setActive(false);
        User saved = userRepository.save(user);
        auditService.log(actorId, AuditAction.USER_BANNED,
                "USER", targetId, "User banned", ip);
        return saved;
    }

    @Transactional
    public User unbanUser(UUID targetId, UUID actorId, String ip) {
        User user = getById(targetId);
        user.setActive(true);
        User saved = userRepository.save(user);
        auditService.log(actorId, AuditAction.USER_UNBANNED,
                "USER", targetId, "User unbanned", ip);
        return saved;
    }

    @Transactional
    public User changeRole(UUID targetId, Role newRole, UUID actorId, String ip) {
        User user = getById(targetId);
        user.setRole(newRole);
        User saved = userRepository.save(user);
        auditService.log(actorId, AuditAction.USER_ROLE_CHANGED,
                "USER", targetId, "Role → " + newRole, ip);
        return saved;
    }
}
