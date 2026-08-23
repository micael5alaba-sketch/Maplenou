package com.maplenou.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret doit faire au moins 256 bits (32 octets). Longueur actuelle : " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(UserDetails userDetails) {
        // On inclut tous les rôles (ROLE_USER, ROLE_SELLER, ROLE_ADMIN…) pour que le client
        // Flutter sache exactement quelles interfaces activer sans appel supplémentaire.
        List<String> roles = userDetails.getAuthorities().stream()
                .map(Object::toString)
                .toList();
        return buildToken(Map.of("type", "access", "roles", roles), userDetails.getUsername(), accessTokenExpirationMs);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(Map.of("type", "refresh"), userDetails.getUsername(), refreshTokenExpirationMs);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaim(token, claims -> claims.get("type", String.class)));
    }

    public String generateMfaToken(String phoneNumber) {
        return buildToken(Map.of("type", "mfa_pending"), phoneNumber, 5 * 60 * 1000L);
    }

    public boolean isMfaToken(String token) {
        return "mfa_pending".equals(extractClaim(token, claims -> claims.get("type", String.class)));
    }

    /** Retourne la date d'expiration d'un token (pour calculer le TTL de blacklist). */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /** Public : utilisé pour rejeter proprement un token MFA expiré avant vérification du code TOTP. */
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Un token JWT expiré fait échouer le parsing signé (ExpiredJwtException) même pour lire un
     * simple claim comme "type" ou "sub". Sans ce rattrapage, isRefreshToken/isMfaToken/
     * extractUsername lèveraient une exception non gérée (500) au lieu de permettre à l'appelant
     * de détecter proprement l'expiration via isTokenExpired/isTokenValid. Jjwt fournit les claims
     * malgré l'expiration via ExpiredJwtException.getClaims().
     */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return resolver.apply(claims);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return resolver.apply(e.getClaims());
        }
    }
}
