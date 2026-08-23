package com.maplenou.backend.notification;

import com.maplenou.backend.notification.dto.RegisterTokenRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Enregistre un token FCM pour l'utilisateur courant.
     * Si le token existe déjà (autre utilisateur ou même utilisateur), on le réassigne :
     * un token ne peut appartenir qu'à un seul compte à la fois.
     */
    @Transactional
    public void register(User user, RegisterTokenRequest req) {
        deviceTokenRepository.findByToken(req.token()).ifPresentOrElse(
                existing -> {
                    // Réassigner à l'utilisateur courant si nécessaire
                    if (!existing.getUser().getId().equals(user.getId())) {
                        existing.setUser(user);
                        existing.setPlatform(req.platform());
                        deviceTokenRepository.save(existing);
                    }
                    // Déjà enregistré pour cet utilisateur — rien à faire
                },
                () -> {
                    DeviceToken token = DeviceToken.builder()
                            .user(user)
                            .token(req.token())
                            .platform(req.platform())
                            .build();
                    deviceTokenRepository.save(token);
                }
        );
    }

    /** Supprime un token FCM (déconnexion d'un appareil précis). */
    @Transactional
    public void unregister(User user, String token) {
        deviceTokenRepository.deleteByTokenAndUserId(token, user.getId());
    }

    /** Supprime tous les tokens FCM de l'utilisateur (déconnexion globale). */
    @Transactional
    public void unregisterAll(User user) {
        deviceTokenRepository.deleteAllByUserId(user.getId());
    }
}
