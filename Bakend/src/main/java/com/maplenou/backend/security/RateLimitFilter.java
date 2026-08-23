package com.maplenou.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rate limiting distribué par IP sur les endpoints d'authentification.
 * Limite configurable via app.rate-limit.auth.max-per-minute (défaut : 10) sur /api/auth/**
 * Implémenté via Redis INCR + EXPIRE (script Lua atomique) :
 * fonctionne correctement même avec plusieurs instances de l'application.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int WINDOW_SECONDS = 60;

    private final RedisTemplate<String, String> redisTemplate;
    private final int maxRequestsPerMinute;

    /**
     * IPs des reverse proxies de confiance (ex: "127.0.0.1,10.0.0.1").
     * X-Forwarded-For n'est pris en compte que si la requête provient d'un proxy listé.
     * En développement local, laisser vide pour désactiver.
     */
    private Set<String> trustedProxyIps = Collections.emptySet();

    public RateLimitFilter(RedisTemplate<String, String> redisTemplate,
                            @Value("${app.rate-limit.auth.max-per-minute:10}") int maxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Value("${app.trusted-proxy-ips:}")
    public void setTrustedProxyIps(String raw) {
        if (raw == null || raw.isBlank()) {
            this.trustedProxyIps = Collections.emptySet();
        } else {
            this.trustedProxyIps = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    /** Script Lua atomique : INCR + EXPIRE (première fois seulement) + retourne le compteur. */
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;
    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return current"
        );
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = "rate:" + extractClientIp(request);

        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(WINDOW_SECONDS)
        );

        if (count != null && count > maxRequestsPerMinute) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Trop de tentatives. Réessayez dans une minute.\",\"status\":429}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * N'utilise X-Forwarded-For que si la requête vient d'un proxy de confiance configuré
     * via app.trusted-proxy-ips. Sinon, utilise directement l'IP de la connexion TCP.
     * Cela évite que n'importe quel client usurpe son IP avec un faux header.
     */
    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyIps.isEmpty() && trustedProxyIps.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
