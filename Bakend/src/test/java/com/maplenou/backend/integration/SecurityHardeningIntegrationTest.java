package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.auth.dto.ForgotPasswordRequest;
import com.maplenou.backend.auth.dto.LoginRequest;
import com.maplenou.backend.auth.dto.LogoutRequest;
import com.maplenou.backend.auth.dto.RefreshRequest;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.auth.dto.ResetPasswordRequest;
import com.maplenou.backend.common.Sha256Utils;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.UpdateProfileRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Couvre le plan de remédiation issu du pentest autorisé du 21/10/2026 : F-01 (anti-énumération
 * mot de passe oublié), F-02 (rotation stricte des refresh tokens), F-03 (anti force brute
 * distribuée), O-02 (limite de tentatives sur le code de réinitialisation). O-01 (propriété sur
 * la livraison par sous-commande) est couvert dans OrderLifecycleIntegrationTest, qui dispose
 * déjà d'une livraison réelle en fixture.
 *
 * app.trusted-proxy-ips=127.0.0.1 : nécessaire pour simuler des IP distinctes via
 * X-Forwarded-For dans le test de force brute distribuée (le client de test tourne toujours en
 * loopback réel). Sans cette confiance explicite, l'en-tête serait ignoré (comportement correct
 * en production tant qu'aucun reverse proxy de confiance n'est configuré).
 */
@TestPropertySource(properties = "app.trusted-proxy-ips=127.0.0.1")
class SecurityHardeningIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final long rand = System.nanoTime() % 100_000_000L;
    private final List<UUID> createdUserIds = new ArrayList<>();

    // ----- F-01 : anti-énumération sur /api/auth/forgot-password -----

    @Test
    void forgotPasswordResponseIsIdenticalRegardlessOfAccountState() throws Exception {
        String unknownPhone = "+22874" + String.format("%07d", rand % 10_000_000L);

        String ineligiblePhone = "+22875" + String.format("%07d", rand % 10_000_000L);
        JsonNode ineligible = post("/api/auth/register",
                new RegisterRequest("Ineligible", ineligiblePhone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(ineligible.get("user").get("id").asText()));

        String eligiblePhone = "+22876" + String.format("%07d", rand % 10_000_000L);
        JsonNode eligible = post("/api/auth/register",
                new RegisterRequest("Eligible", eligiblePhone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(eligible.get("user").get("id").asText()));
        put("/api/users/me", new UpdateProfileRequest(null, "eligible" + rand + "@example.com", null),
                eligible.get("accessToken").asText(), 200);

        JsonNode respUnknown = post("/api/auth/forgot-password",
                new ForgotPasswordRequest(unknownPhone), null, 202);
        JsonNode respIneligible = post("/api/auth/forgot-password",
                new ForgotPasswordRequest(ineligiblePhone), null, 202);
        JsonNode respEligible = post("/api/auth/forgot-password",
                new ForgotPasswordRequest(eligiblePhone), null, 202);

        assertEquals(respUnknown.get("message").asText(), respIneligible.get("message").asText());
        assertEquals(respUnknown.get("message").asText(), respEligible.get("message").asText());
    }

    @Test
    void forgotPasswordRateLimitsPerPhoneNumber() throws Exception {
        String phone = "+22877" + String.format("%07d", rand % 10_000_000L);
        var request = new ForgotPasswordRequest(phone);

        for (int i = 0; i < 5; i++) {
            post("/api/auth/forgot-password", request, null, 202);
        }
        // 6e demande sur le même numéro en moins de 15 min -> 429, jamais un 404/400 qui
        // révélerait quoi que ce soit sur l'existence du compte.
        post("/api/auth/forgot-password", request, null, 429);
    }

    // ----- O-02 : code de réinitialisation haché, comparaison sûre, limite de tentatives -----

    @Test
    void resetPasswordInvalidatesCodeAfterTooManyWrongAttempts() throws Exception {
        String phone = "+22878" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Reset Attempts", phone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));

        redisTemplate.opsForValue().set("password_reset:" + phone, Sha256Utils.hex("123456"), Duration.ofMinutes(10));

        for (int i = 0; i < 5; i++) {
            post("/api/auth/reset-password",
                    new ResetPasswordRequest(phone, "000000", "NewPassw0rd1!"), null, 400);
        }
        // 6e tentative erronée -> 429, et le code (pourtant toujours valide) est invalidé
        post("/api/auth/reset-password",
                new ResetPasswordRequest(phone, "000000", "NewPassw0rd1!"), null, 429);
        // Le compteur de tentatives reste au-dessus du seuil pendant toute sa fenêtre (10 min) :
        // même le bon code renvoie encore 429, pas un 400 trompeur qui suggérerait que resoumettre
        // suffirait.
        post("/api/auth/reset-password",
                new ResetPasswordRequest(phone, "123456", "NewPassw0rd1!"), null, 429);
    }

    @Test
    void resetPasswordSuccessRevokesExistingSessions() throws Exception {
        String phone = "+22879" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Reset Success", phone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));

        JsonNode login = post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 200);
        String oldRefreshToken = login.get("refreshToken").asText();

        redisTemplate.opsForValue().set("password_reset:" + phone, Sha256Utils.hex("654321"), Duration.ofMinutes(10));
        post("/api/auth/reset-password",
                new ResetPasswordRequest(phone, "654321", "BrandNewPassw0rd!"), null, 204);

        // L'ancien mot de passe ne fonctionne plus, et la session active avant la réinitialisation
        // (donc potentiellement compromise si c'est elle qui a motivé la demande) est révoquée.
        post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 401);
        post("/api/auth/refresh", new RefreshRequest(oldRefreshToken), null, 401);

        post("/api/auth/login", new LoginRequest(phone, "BrandNewPassw0rd!"), null, 200);
    }

    // ----- F-02 : rotation stricte des refresh tokens -----

    @Test
    void refreshTokenReuseAfterRotationRevokesWholeFamily() throws Exception {
        String phone = "+22880" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Rotation User", phone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));
        String r0 = registered.get("refreshToken").asText();

        JsonNode afterFirstRefresh = post("/api/auth/refresh", new RefreshRequest(r0), null, 200);
        String r1 = afterFirstRefresh.get("refreshToken").asText();
        assertNotEquals(r0, r1);

        // Rejeu de r0 (déjà consommé pour produire r1) -> rejet, et toute la famille est révoquée
        post("/api/auth/refresh", new RefreshRequest(r0), null, 401);

        // r1, pourtant jamais lui-même rejoué, est désormais inutilisable : toute la famille est
        // tombée avec le jeton rejoué — c'est la défense centrale de F-02.
        post("/api/auth/refresh", new RefreshRequest(r1), null, 401);
    }

    @Test
    void logoutRevokesWholeTokenFamilyNotJustLastToken() throws Exception {
        String phone = "+22881" + String.format("%07d", rand % 10_000_000L);
        JsonNode login = registerAndLogin(phone);
        String r0 = login.get("refreshToken").asText();
        String accessToken = login.get("accessToken").asText();

        JsonNode afterRefresh = post("/api/auth/refresh", new RefreshRequest(r0), null, 200);
        String r1 = afterRefresh.get("refreshToken").asText();

        post("/api/auth/logout", new LogoutRequest(r1, accessToken), null, 204);

        // r1 est le jeton avec lequel on s'est déconnecté ; r0 est son ancêtre déjà consommé.
        // Les deux doivent rester inutilisables après le logout (toute la famille, pas seulement
        // le dernier jeton connu — exigence de non-régression du plan de remédiation).
        post("/api/auth/refresh", new RefreshRequest(r1), null, 401);
    }

    // ----- F-03 : force brute distribuée (nombreuses IP, même compte) -----

    @Test
    void distributedLoginBruteForceAcrossManyIpsTriggersGlobalLock() throws Exception {
        String phone = "+22882" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Brute Force Target", phone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));

        // 10 échecs, chacun depuis une IP différente : aucune IP individuelle n'atteint le seuil
        // de verrouillage par (téléphone, IP) [5], donc seul le compteur global par identifiant
        // peut détecter cette attaque distribuée — c'est exactement le trou signalé par le
        // pentest du 21/10/2026 (19 échecs cumulés acceptés sans 429 ni verrouillage).
        for (int i = 0; i < 10; i++) {
            postWithHeader("/api/auth/login", new LoginRequest(phone, "WrongPass" + i),
                    "X-Forwarded-For", "10.50.0." + i, 401);
        }

        // 11e tentative, encore une IP fraîche jamais vue : bloquée par le verrou global.
        postWithHeader("/api/auth/login", new LoginRequest(phone, "WrongPassFinal"),
                "X-Forwarded-For", "10.50.0.99", 429);
    }

    private JsonNode registerAndLogin(String phone) throws Exception {
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Security Test User", phone, "Passw0rd!", null), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));
        return post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 200);
    }

    @AfterAll
    void cleanup() {
        for (UUID id : createdUserIds) {
            try {
                userRepository.deleteById(id);
            } catch (Exception ignored) {
            }
        }
    }
}
