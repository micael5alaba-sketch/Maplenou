package com.maplenou.backend.security;

import com.maplenou.backend.common.Sha256Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Blacklist de tokens JWT stockée dans Redis.
 * Utilisée pour invalider immédiatement un refresh token lors du logout,
 * et pour bloquer un access token révoqué avant son expiration naturelle.
 *
 * Clé Redis : "blacklist:{sha256(token)}"
 * TTL = durée de vie restante du token → la clé disparaît automatiquement.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";
    private final RedisTemplate<String, String> redisTemplate;

    /** Ajoute un token à la blacklist jusqu'à son expiration. */
    public void blacklist(String token, long tokenExpiryEpochMs) {
        long ttlSeconds = (tokenExpiryEpochMs - System.currentTimeMillis()) / 1000;
        if (ttlSeconds <= 0) return; // token déjà expiré, inutile de le stocker
        redisTemplate.opsForValue().set(
                PREFIX + hash(token), "1", Duration.ofSeconds(ttlSeconds));
    }

    /** Retourne true si le token a été révoqué. */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + hash(token)));
    }

    private String hash(String token) {
        return Sha256Utils.hex(token);
    }
}
