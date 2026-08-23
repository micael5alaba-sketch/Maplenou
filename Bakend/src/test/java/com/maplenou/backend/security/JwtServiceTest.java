package com.maplenou.backend.security;

import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // 32+ octets requis (validé au démarrage par JwtService lui-même).
    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900_000L, 604_800_000L);
        user = User.builder()
                .fullName("Test User")
                .phoneNumber("+22890000001")
                .passwordHash("hash")
                .role(null)
                .active(true)
                .build();
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService("too-short-secret", 900_000L, 604_800_000L));
    }

    @Test
    void accessTokenCarriesUsernameAndRoles() {
        String token = jwtService.generateAccessToken(user);
        assertEquals(user.getUsername(), jwtService.extractUsername(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isMfaToken(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void refreshTokenIsFlaggedAsRefresh() {
        String token = jwtService.generateRefreshToken(user);
        assertTrue(jwtService.isRefreshToken(token));
        assertFalse(jwtService.isMfaToken(token));
    }

    @Test
    void mfaTokenIsFlaggedAsMfaAndNotAsAccessOrRefresh() {
        String token = jwtService.generateMfaToken(user.getPhoneNumber());
        assertTrue(jwtService.isMfaToken(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals(user.getPhoneNumber(), jwtService.extractUsername(token));
    }

    @Test
    void tokenIsInvalidForADifferentUser() {
        String token = jwtService.generateAccessToken(user);
        User otherUser = User.builder()
                .fullName("Other")
                .phoneNumber("+22890000002")
                .passwordHash("hash")
                .active(true)
                .build();
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void expiredTokenIsInvalidWithoutThrowing() {
        // Un token expiré doit être détecté proprement (false), jamais lever d'exception :
        // sinon AuthService.refresh()/verifyMfaLogin() planteraient en 500 au lieu d'un 401 propre.
        JwtService shortLivedJwt = new JwtService(SECRET, -1000L, 604_800_000L);
        String token = shortLivedJwt.generateAccessToken(user);

        assertTrue(shortLivedJwt.isTokenExpired(token));
        assertFalse(shortLivedJwt.isTokenValid(token, user));
        assertEquals(user.getUsername(), shortLivedJwt.extractUsername(token));
    }

    @Test
    void expiredRefreshTokenIsStillIdentifiableAsRefreshType() {
        JwtService shortLivedJwt = new JwtService(SECRET, 900_000L, -1000L);
        String token = shortLivedJwt.generateRefreshToken(user);

        assertTrue(shortLivedJwt.isRefreshToken(token));
        assertTrue(shortLivedJwt.isTokenExpired(token));
    }

    @Test
    void mfaTokenIsNotExpiredImmediatelyAfterIssuance() {
        String mfaToken = jwtService.generateMfaToken(user.getPhoneNumber());
        assertTrue(jwtService.isMfaToken(mfaToken));
        assertFalse(jwtService.isTokenExpired(mfaToken));
    }

    @Test
    void tamperedTokenFailsSignatureVerification() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";
        assertThrows(JwtException.class, () -> jwtService.extractUsername(tampered));
    }
}
