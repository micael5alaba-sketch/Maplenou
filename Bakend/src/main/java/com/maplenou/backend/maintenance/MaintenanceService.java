package com.maplenou.backend.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Mode maintenance plateforme : coupe tout le trafic API (sauf l'admin lui-même, voir
 * {@link MaintenanceFilter}) en un clic depuis le back-office, pour une intervention
 * (migration risquée, incident, bascule de paiement...). État stocké dans Redis (pas en base)
 * pour rester lisible même si la base de données est justement la cible de l'intervention, et
 * pour être immédiatement visible par toutes les instances de l'application sans redéploiement.
 */
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private static final String ENABLED_KEY = "platform:maintenance:enabled";
    private static final String MESSAGE_KEY = "platform:maintenance:message";
    private static final String DEFAULT_MESSAGE =
            "Maplenou est actuellement en maintenance. Merci de réessayer dans quelques instants.";

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isEnabled() {
        return "true".equals(redisTemplate.opsForValue().get(ENABLED_KEY));
    }

    public String getMessage() {
        String message = redisTemplate.opsForValue().get(MESSAGE_KEY);
        return (message != null && !message.isBlank()) ? message : DEFAULT_MESSAGE;
    }

    public void enable(String message) {
        redisTemplate.opsForValue().set(ENABLED_KEY, "true");
        redisTemplate.opsForValue().set(MESSAGE_KEY,
                (message != null && !message.isBlank()) ? message : DEFAULT_MESSAGE);
    }

    public void disable() {
        redisTemplate.delete(ENABLED_KEY);
        redisTemplate.delete(MESSAGE_KEY);
    }
}
