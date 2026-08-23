package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.maplenou.backend.audit.AuditLogRepository;
import com.maplenou.backend.auth.dto.*;
import com.maplenou.backend.notification.DevicePlatform;
import com.maplenou.backend.notification.dto.RegisterTokenRequest;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.ChangeRoleRequest;
import com.maplenou.backend.user.dto.CreateAddressRequest;
import com.maplenou.backend.user.dto.UpdateAddressRequest;
import com.maplenou.backend.user.dto.UpdateProfileRequest;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthAndUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private final long rand = System.nanoTime() % 100_000_000L;
    private final List<UUID> createdUserIds = new ArrayList<>();

    @Test
    void registerLoginRefreshLogoutFlow() throws Exception {
        String phone = "+22892" + String.format("%07d", rand % 10_000_000L);

        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Auth Flow User", phone, "Passw0rd!"), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));
        assertNotNull(registered.get("accessToken").asText());
        assertNotNull(registered.get("refreshToken").asText());

        // Doublon de téléphone -> 409
        post("/api/auth/register", new RegisterRequest("Dup", phone, "Passw0rd!"), null, 409);

        // Mauvais mot de passe -> 401
        post("/api/auth/login", new LoginRequest(phone, "WrongPass1"), null, 401);

        JsonNode login = post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 200);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        // Rafraîchissement
        JsonNode refreshed = post("/api/auth/refresh", new RefreshRequest(refreshToken), null, 200);
        assertNotNull(refreshed.get("accessToken").asText());

        // Déconnexion révoque le refresh token
        post("/api/auth/logout", new LogoutRequest(refreshToken, accessToken), null, 204);
        post("/api/auth/refresh", new RefreshRequest(refreshToken), null, 401);
    }

    @Test
    void twoFactorSetupActivateLoginChallengeAndDisable() throws Exception {
        String phone = "+22893" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("2FA User", phone, "Passw0rd!"), null, 201);
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));
        String token = registered.get("accessToken").asText();

        JsonNode setup = post("/api/auth/2fa/setup", null, token, 200);
        String secret = setup.get("secret").asText();
        assertTrue(setup.get("otpAuthUri").asText().contains(secret));

        String code = totpCodeFor(secret);
        post("/api/auth/2fa/activate", new VerifyTotpRequest(code), token, 200);

        // Le login exige désormais une étape MFA
        JsonNode loginChallenge = post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 200);
        assertTrue(loginChallenge.has("mfaToken"), "un challenge MFA doit être renvoyé : " + loginChallenge);
        String mfaToken = loginChallenge.get("mfaToken").asText();

        String loginCode = totpCodeFor(secret);
        JsonNode fullAuth = post("/api/auth/2fa/verify-login", new MfaLoginRequest(mfaToken, loginCode), null, 200);
        String newAccessToken = fullAuth.get("accessToken").asText();
        assertNotNull(newAccessToken);

        // Anti-rejeu : le même token MFA ne peut pas être réutilisé
        post("/api/auth/2fa/verify-login", new MfaLoginRequest(mfaToken, loginCode), null, 401);

        // Désactivation nécessite un code TOTP valide (DELETE avec corps -> exchange direct)
        String disableCode = totpCodeFor(secret);
        exchange("DELETE", "/api/auth/2fa/disable", new VerifyTotpRequest(disableCode), newAccessToken, 204);

        // La 2FA n'est plus exigée au prochain login
        JsonNode loginAfterDisable = post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 200);
        assertTrue(loginAfterDisable.has("accessToken"));
    }

    @Test
    void userProfileAndAddresses() throws Exception {
        String phone = "+22894" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Profile User", phone, "Passw0rd!"), null, 201);
        String token = registered.get("accessToken").asText();
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));

        JsonNode me = get("/api/users/me", token, 200);
        assertEquals(phone, me.get("phoneNumber").asText());

        JsonNode updated = put("/api/users/me",
                new UpdateProfileRequest("Nouveau Nom", "user@example.com"), token, 200);
        assertEquals("Nouveau Nom", updated.get("fullName").asText());

        JsonNode address = post("/api/users/me/addresses",
                new CreateAddressRequest("Maison", "Lomé", "Bè", "Près du marché", 6.13, 1.22, true),
                token, 201);
        UUID addressId = UUID.fromString(address.get("id").asText());

        JsonNode list = get("/api/users/me/addresses", token, 200);
        assertTrue(list.isArray() && list.size() >= 1);

        JsonNode updatedAddr = put("/api/users/me/addresses/" + addressId,
                new UpdateAddressRequest("Bureau", "Lomé", "Adidogomé", null, null, null, null),
                token, 200);
        assertEquals("Bureau", updatedAddr.get("label").asText());

        patch("/api/users/me/addresses/" + addressId + "/default", null, token, 204);
        delete("/api/users/me/addresses/" + addressId, token, 204);
    }

    @Test
    void adminUserManagement() throws Exception {
        String adminToken = login("+22890000000", "Micael2005@");

        String phone = "+22895" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Ban Target", phone, "Passw0rd!"), null, 201);
        UUID banUserId = UUID.fromString(registered.get("user").get("id").asText());
        createdUserIds.add(banUserId);

        JsonNode listUsers = get("/api/admin/users", adminToken, 200);
        assertTrue(listUsers.get("content").isArray());

        JsonNode fetched = get("/api/admin/users/" + banUserId, adminToken, 200);
        assertEquals(phone, fetched.get("phoneNumber").asText());

        JsonNode banned = patch("/api/admin/users/" + banUserId + "/ban", null, adminToken, 200);
        assertFalse(banned.get("active").asBoolean());

        // Un compte banni ne peut plus se connecter
        post("/api/auth/login", new LoginRequest(phone, "Passw0rd!"), null, 401);

        JsonNode unbanned = patch("/api/admin/users/" + banUserId + "/unban", null, adminToken, 200);
        assertTrue(unbanned.get("active").asBoolean());

        // Changement de rôle vers DELIVERY_AGENT
        String rolePhone = "+22896" + String.format("%07d", rand % 10_000_000L);
        JsonNode roleUser = post("/api/auth/register",
                new RegisterRequest("Role Target", rolePhone, "Passw0rd!"), null, 201);
        UUID roleChangeUserId = UUID.fromString(roleUser.get("user").get("id").asText());
        createdUserIds.add(roleChangeUserId);

        JsonNode changed = patch("/api/admin/users/" + roleChangeUserId + "/role",
                new ChangeRoleRequest(Role.DELIVERY_AGENT), adminToken, 200);
        assertEquals("DELIVERY_AGENT", changed.get("role").asText());

        // L'audit log doit être consultable (contient au moins ces actions admin)
        JsonNode audit = get("/api/admin/audit", adminToken, 200);
        assertTrue(audit.get("content").isArray());
        JsonNode auditByEntity = get("/api/admin/audit/entity/" + banUserId, adminToken, 200);
        assertTrue(auditByEntity.get("content").isArray());
    }

    @Test
    void gdprSelfDeleteAndAdminAnonymize() throws Exception {
        String adminToken = login("+22890000000", "Micael2005@");

        String selfPhone = "+22897" + String.format("%07d", rand % 10_000_000L);
        JsonNode self = post("/api/auth/register",
                new RegisterRequest("Self Delete", selfPhone, "Passw0rd!"), null, 201);
        String selfToken = self.get("accessToken").asText();
        createdUserIds.add(UUID.fromString(self.get("user").get("id").asText()));

        delete("/api/users/me", selfToken, 204);
        // Le compte est désactivé -> connexion impossible désormais
        post("/api/auth/login", new LoginRequest(selfPhone, "Passw0rd!"), null, 401);

        String anonPhone = "+22898" + String.format("%07d", rand % 10_000_000L);
        JsonNode anon = post("/api/auth/register",
                new RegisterRequest("Anon Target", anonPhone, "Passw0rd!"), null, 201);
        UUID gdprAnonymizeUserId = UUID.fromString(anon.get("user").get("id").asText());
        createdUserIds.add(gdprAnonymizeUserId);

        post("/api/admin/gdpr/users/" + gdprAnonymizeUserId + "/anonymize", null, adminToken, 204);
        JsonNode fetched = get("/api/admin/users/" + gdprAnonymizeUserId, adminToken, 200);
        assertEquals("[Supprimé]", fetched.get("fullName").asText());
    }

    @Test
    void deviceTokenRegistrationLifecycle() throws Exception {
        String phone = "+22899" + String.format("%07d", rand % 10_000_000L);
        JsonNode registered = post("/api/auth/register",
                new RegisterRequest("Device Token User", phone, "Passw0rd!"), null, 201);
        String token = registered.get("accessToken").asText();
        createdUserIds.add(UUID.fromString(registered.get("user").get("id").asText()));

        String fcmToken1 = "fcm-token-android-" + rand;
        String fcmToken2 = "fcm-token-web-" + rand;

        post("/api/notifications/tokens", new RegisterTokenRequest(fcmToken1, DevicePlatform.ANDROID), token, 201);
        post("/api/notifications/tokens", new RegisterTokenRequest(fcmToken2, DevicePlatform.WEB), token, 201);
        // Ré-enregistrer le même token (même utilisateur, même plateforme) est idempotent
        post("/api/notifications/tokens", new RegisterTokenRequest(fcmToken1, DevicePlatform.ANDROID), token, 201);

        delete("/api/notifications/tokens/" + fcmToken1, token, 204);
        delete("/api/notifications/tokens", token, 204);
    }

    @Test
    void adminKpiDashboard() throws Exception {
        String adminToken = login("+22890000000", "Micael2005@");

        JsonNode defaultDashboard = get("/api/admin/kpis", adminToken, 200);
        assertTrue(defaultDashboard.has("revenue"));
        assertTrue(defaultDashboard.has("orders"));
        assertTrue(defaultDashboard.has("users"));
        assertTrue(defaultDashboard.has("shops"));
        assertTrue(defaultDashboard.has("deliveries"));

        JsonNode allTime = get("/api/admin/kpis?period=ALL_TIME", adminToken, 200);
        assertTrue(allTime.get("users").get("total").asLong() >= 1);

        get("/api/admin/kpis?period=TODAY", adminToken, 200);
        get("/api/admin/kpis?period=LAST_7_DAYS", adminToken, 200);

        // Non-admin refusé
        JsonNode buyer = post("/api/auth/register",
                new RegisterRequest("KPI Peeker", "+228991" + String.format("%06d", rand % 1_000_000L), "Passw0rd!"),
                null, 201);
        createdUserIds.add(UUID.fromString(buyer.get("user").get("id").asText()));
        get("/api/admin/kpis", buyer.get("accessToken").asText(), 403);
    }

    private String totpCodeFor(String secret) throws Exception {
        return new DefaultCodeGenerator().generate(secret, new SystemTimeProvider().getTime() / 30);
    }

    @AfterAll
    void cleanup() {
        for (UUID id : createdUserIds) {
            try {
                // Un utilisateur peut être l'acteur d'une action d'audit (ex: auto-activation 2FA) ;
                // audit_logs.actor_id référence users(id), il faut purger avant de supprimer l'utilisateur.
                auditLogRepository.deleteAll(
                        auditLogRepository.findByActorIdOrderByCreatedAtDesc(id, org.springframework.data.domain.Pageable.unpaged()));
                userRepository.deleteById(id);
            } catch (Exception ignored) { }
        }
    }
}
