package com.maplenou.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rôles et accès du back-office (document "Rôles et accès au tableau de bord") : matrice des
 * droits appliquée CÔTÉ SERVEUR, première connexion forcée (mot de passe + MFA), gestion des
 * comptes du personnel, codes de récupération MFA.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StaffRbacIntegrationTest extends AbstractIntegrationTest {

    private String superToken;
    private StaffSession support;
    private StaffSession deliveryAdmin;
    private StaffSession deliveryAdminWithPricing;
    private StaffSession auditor;

    @BeforeAll
    void setUp() throws Exception {
        superToken = login("+22890000000", "Micael2005@");
        support = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        deliveryAdmin = createStaffWithMfa(superToken, "DELIVERY_ADMIN", false);
        deliveryAdminWithPricing = createStaffWithMfa(superToken, "DELIVERY_ADMIN", true);
        auditor = createStaffWithMfa(superToken, "AUDITOR", false);
    }

    // ----- Première connexion : mot de passe + MFA imposés avant l'accès -----

    @Test
    @Order(1)
    void firstLoginForcesPasswordChangeThenMfaBeforeAnyAccess() throws Exception {
        String phone = "+22861" + String.format("%07d", System.nanoTime() % 10_000_000L);
        JsonNode created = post("/api/admin/staff",
                Map.of("fullName", "Nouvelle recrue", "phoneNumber", phone, "email", "recrue" + System.nanoTime() + "@maplenou.test", "role", "SUPPORT_AGENT"), superToken, 201);
        String tempPassword = created.get("temporaryPassword").asText();
        assertTrue(tempPassword.length() >= 12);
        assertTrue(created.get("staff").get("mustChangePassword").asBoolean());

        String token = login(phone, tempPassword);

        // Tableau de bord et API refusés tant que l'onboarding n'est pas terminé
        JsonNode blocked = get("/api/admin/payouts", token, 403);
        assertEquals("STAFF_ONBOARDING_REQUIRED", blocked.get("code").asText());
        assertTrue(blocked.get("mustChangePassword").asBoolean());
        assertTrue(blocked.get("mfaSetupRequired").asBoolean());
        // ... mais le profil reste lisible pour que l'écran sache quoi demander
        JsonNode me = get("/api/users/me", token, 200);
        assertTrue(me.get("mustChangePassword").asBoolean());

        // Après le changement de mot de passe, la MFA reste exigée
        patch("/api/users/me/password", Map.of("currentPassword", tempPassword, "newPassword", "Recrue1password"), token, 204);
        JsonNode stillBlocked = get("/api/admin/payouts", token, 403);
        assertEquals("STAFF_ONBOARDING_REQUIRED", stillBlocked.get("code").asText());
        assertFalse(stillBlocked.get("mustChangePassword").asBoolean());
        assertTrue(stillBlocked.get("mfaSetupRequired").asBoolean());

        // MFA activée : l'accès s'ouvre, avec les permissions du rôle
        String secret = post("/api/auth/2fa/setup", null, token, 200).get("secret").asText();
        String code = new dev.samstevens.totp.code.DefaultCodeGenerator().generate(secret,
                new dev.samstevens.totp.time.SystemTimeProvider().getTime() / 30);
        post("/api/auth/2fa/activate", Map.of("code", code), token, 200);
        get("/api/admin/payouts", token, 200);
        JsonNode meAfter = get("/api/users/me", token, 200);
        assertEquals("SUPPORT_AGENT", meAfter.get("role").asText());
        assertTrue(meAfter.get("permissions").toString().contains("PAYOUTS_PREPARE"));
        assertFalse(meAfter.get("permissions").toString().contains("PAYOUTS_APPROVE"));

        // La MFA ne peut pas être désactivée par un compte du personnel
        exchange("DELETE", "/api/auth/2fa/disable", Map.of("code", code), token, 403);
    }

    // ----- Matrice des accès (§7) : refus côté serveur -----

    @Test
    @Order(2)
    void supportAgentCanSupportButNeverDeliverNorApproveNorManageStaff() throws Exception {
        get("/api/admin/support-tickets", support.token(), 200);
        get("/api/admin/payouts", support.token(), 200);
        get("/api/sub-orders/admin", support.token(), 200);
        get("/api/admin/shops", support.token(), 200);

        // Affecter une livraison, créer un livreur, tarification : refusés
        patch("/api/deliveries/" + UUID.randomUUID() + "/assign", Map.of("agentId", UUID.randomUUID().toString()), support.token(), 403);
        post("/api/admin/delivery-agents", Map.of("fullName", "Livreur Test", "phoneNumber", "+22899999999", "password", "Passw0rd!"), support.token(), 403);
        post("/api/delivery-pricing-tiers", Map.of("maxDistanceKm", "5", "price", "1000"), support.token(), 403);
        // Personnel, journal d'audit complet, utilisateurs : refusés
        get("/api/admin/staff", support.token(), 403);
        get("/api/admin/audit", support.token(), 403);
        get("/api/admin/users", support.token(), 403);
        post("/api/admin/staff", Map.of("fullName", "Intrus", "phoneNumber", "+22899999990", "email", "intrus@maplenou.test", "role", "ADMIN"), support.token(), 403);
        // Catégories / CMS / codes promo : Super Admin uniquement
        post("/api/admin/pages", Map.of("slug", "x-test", "title", "T", "body", "B", "published", false), support.token(), 403);
        // Retraits : jamais d'approbation, de rejet ni de paiement
        post("/api/admin/payouts/" + UUID.randomUUID() + "/approve", Map.of("mfaCode", support.code()), support.token(), 403);
        patch("/api/admin/payouts/" + UUID.randomUUID() + "/reject", Map.of("notes", "non"), support.token(), 403);
        patch("/api/admin/payouts/" + UUID.randomUUID() + "/paid", null, support.token(), 403);
    }

    @Test
    @Order(3)
    void deliveryAdminManagesDeliveriesOnly() throws Exception {
        get("/api/deliveries/pending", deliveryAdmin.token(), 200);
        get("/api/deliveries", deliveryAdmin.token(), 200);
        get("/api/admin/delivery-agents", deliveryAdmin.token(), 200);
        get("/api/admin/delivery-agents/locations", deliveryAdmin.token(), 200);
        get("/api/admin/courier-documents", deliveryAdmin.token(), 200);
        get("/api/admin/courier-earnings", deliveryAdmin.token(), 200);

        // Support, retraits, vendeurs, utilisateurs, audit, personnel : modules masqués ET API refusée
        get("/api/admin/support-tickets", deliveryAdmin.token(), 403);
        get("/api/admin/conversations/support", deliveryAdmin.token(), 403);
        get("/api/admin/payouts", deliveryAdmin.token(), 403);
        get("/api/admin/courier-withdrawals", deliveryAdmin.token(), 403);
        get("/api/sellers/admin", deliveryAdmin.token(), 403);
        get("/api/admin/shops", deliveryAdmin.token(), 403);
        get("/api/admin/users", deliveryAdmin.token(), 403);
        get("/api/admin/audit", deliveryAdmin.token(), 403);
        get("/api/admin/staff", deliveryAdmin.token(), 403);
        get("/api/sub-orders/admin", deliveryAdmin.token(), 403);

        // Tarification : seulement si attribuée par le Super Admin
        Map<String, Object> tier = Map.of("maxDistanceKm", "999.00", "price", "1.00");
        post("/api/delivery-pricing-tiers", tier, deliveryAdmin.token(), 403);
        JsonNode created = post("/api/delivery-pricing-tiers", tier, deliveryAdminWithPricing.token(), 201);
        delete("/api/delivery-pricing-tiers/" + created.get("id").asText(), deliveryAdminWithPricing.token(), 204);
    }

    @Test
    @Order(4)
    void auditorReadsEverythingButModifiesNothing() throws Exception {
        get("/api/admin/audit", auditor.token(), 200);
        get("/api/admin/users", auditor.token(), 200);
        get("/api/admin/payouts", auditor.token(), 200);
        get("/api/admin/courier-withdrawals", auditor.token(), 200);
        get("/api/sub-orders/admin", auditor.token(), 200);
        get("/api/admin/shops", auditor.token(), 200);
        get("/api/admin/support-tickets", auditor.token(), 200);
        get("/api/admin/pages", auditor.token(), 200);
        get("/api/deliveries/pending", auditor.token(), 200);

        // Aucune écriture, quelle qu'elle soit : refusée côté serveur
        post("/api/admin/payouts/generate", Map.of("periodStart", "2026-01-01T00:00:00Z", "periodEnd", "2026-01-02T00:00:00Z"), auditor.token(), 403);
        patch("/api/admin/payouts/" + UUID.randomUUID() + "/prepare", null, auditor.token(), 403);
        post("/api/admin/payouts/" + UUID.randomUUID() + "/approve", Map.of("mfaCode", auditor.code()), auditor.token(), 403);
        patch("/api/deliveries/" + UUID.randomUUID() + "/assign", Map.of("agentId", UUID.randomUUID().toString()), auditor.token(), 403);
        patch("/api/admin/support-tickets/" + UUID.randomUUID() + "/resolve", Map.of("status", "RESOLVED", "resolutionNotes", "x"), auditor.token(), 403);
        patch("/api/admin/shops/" + UUID.randomUUID() + "/status", Map.of("status", "APPROVED"), auditor.token(), 403);
        patch("/api/admin/users/" + UUID.randomUUID() + "/ban", null, auditor.token(), 403);
        post("/api/admin/pages", Map.of("slug", "x-test", "title", "T", "body", "B", "published", false), auditor.token(), 403);
        post("/api/admin/staff", Map.of("fullName", "Intrus", "phoneNumber", "+22899999991", "email", "intrus2@maplenou.test", "role", "ADMIN"), auditor.token(), 403);
    }

    @Test
    @Order(5)
    void dashboardShowsOnlyTheSectionsOfTheRole() throws Exception {
        JsonNode logistics = get("/api/admin/kpis", deliveryAdmin.token(), 200);
        assertTrue(logistics.get("revenue").isNull(), "pas de chiffre d'affaires pour l'admin des livraisons");
        assertTrue(logistics.get("users").isNull());
        assertFalse(logistics.get("deliveries").isNull());

        JsonNode supportView = get("/api/admin/kpis", support.token(), 200);
        assertFalse(supportView.get("orders").isNull());
        assertTrue(supportView.get("users").isNull());

        JsonNode full = get("/api/admin/kpis", superToken, 200);
        assertFalse(full.get("revenue").isNull());
        assertFalse(full.get("users").isNull());
    }

    // ----- Gestion des comptes du personnel -----

    @Test
    @Order(6)
    void superAdminDeactivatesAnAccountImmediatelyAndResetsItsAccess() throws Exception {
        StaffSession temp = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);
        get("/api/admin/payouts", temp.token(), 200);

        // Désactivation : jeton refusé immédiatement, connexion impossible
        patch("/api/admin/staff/" + temp.userId() + "/active", Map.of("active", false, "justification", "Départ de l'équipe"), superToken, 200);
        get("/api/users/me", temp.token(), 401);
        post("/api/auth/login", Map.of("phoneNumber", temp.phone(), "password", temp.password()), null, 401);

        // Réactivation puis réinitialisation : mot de passe initial + MFA à reconfigurer
        patch("/api/admin/staff/" + temp.userId() + "/active", Map.of("active", true), superToken, 200);
        JsonNode reset = post("/api/admin/staff/" + temp.userId() + "/reset-password", null, superToken, 200);
        String newTemp = reset.get("temporaryPassword").asText();
        String token = login(temp.phone(), newTemp);
        assertEquals("STAFF_ONBOARDING_REQUIRED", get("/api/admin/payouts", token, 403).get("code").asText());
    }

    @Test
    @Order(7)
    void staffRolesAreManagedOnlyThroughTheStaffApi() throws Exception {
        // Le changement de rôle générique ne peut ni attribuer ni retirer un rôle du personnel
        patch("/api/admin/users/" + support.userId() + "/role", Map.of("role", "DELIVERY_AGENT"), superToken, 403);
        patch("/api/admin/users/" + support.userId() + "/ban", null, superToken, 403);

        // Garde-fous : pas d'auto-désactivation, pas d'auto-changement de rôle
        String superId = get("/api/users/me", superToken, 200).get("id").asText();
        patch("/api/admin/staff/" + superId + "/active", Map.of("active", false), superToken, 403);
        patch("/api/admin/staff/" + superId, Map.of("role", "AUDITOR"), superToken, 403);

        // Un changement de rôle via l'API du personnel prend effet immédiatement
        patch("/api/admin/staff/" + auditor.userId(), Map.of("role", "SUPPORT_AGENT"), superToken, 200);
        get("/api/admin/support-tickets", auditor.token(), 200);
        patch("/api/admin/staff/" + auditor.userId(), Map.of("role", "AUDITOR"), superToken, 200);

        // La tarification ne s'attribue qu'à l'Administrateur des livraisons
        patch("/api/admin/staff/" + support.userId(), Map.of("deliveryPricingGranted", true), superToken, 400);

        JsonNode list = get("/api/admin/staff?size=100", superToken, 200);
        assertTrue(list.get("content").size() >= 4);
    }

    @Test
    @Order(8)
    void refusedAttemptsOfStaffAreTracedInTheAuditLog() throws Exception {
        get("/api/admin/audit", support.token(), 403);
        JsonNode audit = get("/api/admin/audit?size=100&sort=createdAt,desc", superToken, 200);
        boolean traced = false;
        for (JsonNode entry : audit.get("content")) {
            if ("ACCESS_DENIED".equals(entry.get("action").asText())
                    && entry.get("details").asText().contains("/api/admin/audit")) {
                traced = true;
                break;
            }
        }
        assertTrue(traced, "un accès refusé du personnel doit apparaître dans le journal d'audit");
    }

    // ----- Codes de récupération MFA -----

    @Test
    @Order(9)
    void recoveryCodeLogsInOnlyOnce() throws Exception {
        String phone = "+22862" + String.format("%07d", System.nanoTime() % 10_000_000L);
        JsonNode created = post("/api/admin/staff",
                Map.of("fullName", "Perte de téléphone", "phoneNumber", phone, "email", "perte" + System.nanoTime() + "@maplenou.test", "role", "AUDITOR"), superToken, 201);
        String temp = created.get("temporaryPassword").asText();
        String token = login(phone, temp);
        patch("/api/users/me/password", Map.of("currentPassword", temp, "newPassword", "Recup1password"), token, 204);
        String secret = post("/api/auth/2fa/setup", null, token, 200).get("secret").asText();
        String code = new dev.samstevens.totp.code.DefaultCodeGenerator().generate(secret,
                new dev.samstevens.totp.time.SystemTimeProvider().getTime() / 30);
        JsonNode activated = post("/api/auth/2fa/activate", Map.of("code", code), token, 200);
        String recoveryCode = activated.get("recoveryCodes").get(0).asText();
        assertNotNull(recoveryCode);

        // Téléphone perdu : le code de récupération remplace le TOTP, une seule fois
        String mfa1 = post("/api/auth/login", Map.of("phoneNumber", phone, "password", "Recup1password"), null, 200)
                .get("mfaToken").asText();
        post("/api/auth/2fa/verify-login", Map.of("mfaToken", mfa1, "code", recoveryCode), null, 200);

        String mfa2 = post("/api/auth/login", Map.of("phoneNumber", phone, "password", "Recup1password"), null, 200)
                .get("mfaToken").asText();
        post("/api/auth/2fa/verify-login", Map.of("mfaToken", mfa2, "code", recoveryCode), null, 401);
    }

    // ----- Anti-devinette du code MFA -----

    @Test
    @Order(10)
    void repeatedWrongMfaCodesLockTheAccountEvenForTheRightCode() throws Exception {
        StaffSession victim = createStaffWithMfa(superToken, "AUDITOR", false);
        // Les jetons MFA sont signés à la seconde : un jeton émis dans la même seconde que celui déjà
        // consommé par createStaffWithMfa serait identique (donc déjà "utilisé").
        Thread.sleep(1100);
        String mfaToken = post("/api/auth/login", Map.of("phoneNumber", victim.phone(), "password", victim.password()), null, 200)
                .get("mfaToken").asText();

        for (int i = 0; i < 5; i++) {
            post("/api/auth/2fa/verify-login", Map.of("mfaToken", mfaToken, "code", "000000"), null, 401);
        }
        // Verrouillé : même le bon code est refusé (429), le temps du verrou
        post("/api/auth/2fa/verify-login", Map.of("mfaToken", mfaToken, "code", victim.code()), null, 429);
    }

    @Test
    @Order(11)
    void mfaSetupRoutesRequireAnAuthenticatedUser() throws Exception {
        post("/api/auth/2fa/setup", null, null, 401);
        post("/api/auth/2fa/activate", Map.of("code", "123456"), null, 401);
        post("/api/auth/2fa/recovery-codes", Map.of("code", "123456"), null, 401);
    }

    // ----- Identifiants : email + mot de passe, modifiables uniquement par le Super Admin -----

    @Test
    @Order(12)
    void staffLogsInWithEmailAndSuperAdminCanChangeTheIdentifiers() throws Exception {
        StaffSession s = createStaffWithMfa(superToken, "SUPPORT_AGENT", false);

        // Connexion par email (étape MFA ensuite), au lieu du numéro de téléphone
        Thread.sleep(1100);
        JsonNode viaEmail = post("/api/auth/login", Map.of("phoneNumber", s.email(), "password", s.password()), null, 200);
        assertTrue(viaEmail.get("mfaRequired").asBoolean());
        post("/api/auth/2fa/verify-login", Map.of("mfaToken", viaEmail.get("mfaToken").asText(), "code", s.code()), null, 200);

        // Email en doublon refusé
        patch("/api/admin/staff/" + s.userId(), Map.of("email", auditor.email()), superToken, 409);

        // Le personnel ne peut pas modifier ses propres identifiants via l'API du personnel
        patch("/api/admin/staff/" + s.userId(), Map.of("email", "moi@maplenou.test"), s.token(), 403);

        // Le Super Admin change l'email et le téléphone : l'ancien identifiant ne marche plus,
        // les sessions ouvertes sont révoquées
        String newEmail = "nouveau" + System.nanoTime() + "@maplenou.test";
        String newPhone = "+22863" + String.format("%07d", System.nanoTime() % 10_000_000L);
        JsonNode updated = patch("/api/admin/staff/" + s.userId(), Map.of("email", newEmail, "phoneNumber", newPhone), superToken, 200);
        assertEquals(newEmail, updated.get("email").asText());
        assertEquals(newPhone, updated.get("phoneNumber").asText());
        post("/api/auth/login", Map.of("phoneNumber", s.email(), "password", s.password()), null, 401);
        post("/api/auth/login", Map.of("phoneNumber", s.phone(), "password", s.password()), null, 401);
        JsonNode withNew = post("/api/auth/login", Map.of("phoneNumber", newEmail, "password", s.password()), null, 200);
        assertTrue(withNew.get("mfaRequired").asBoolean());
    }

    // ----- Mode "Voir en tant que" du Super Admin -----

    @Test
    @Order(13)
    void superAdminCanPreviewEachRoleDashboardButNobodyElse() throws Exception {
        // Permissions de chaque rôle (source de vérité du back-office)
        JsonNode roles = get("/api/admin/staff/roles", superToken, 200);
        assertTrue(roles.get("DELIVERY_ADMIN").toString().contains("DELIVERIES_WRITE"));
        assertFalse(roles.get("DELIVERY_ADMIN").toString().contains("PAYOUTS_READ"));
        assertFalse(roles.get("SUPPORT_AGENT").toString().contains("PAYOUTS_APPROVE"));
        assertTrue(roles.get("ADMIN").toString().contains("STAFF_MANAGE"));
        get("/api/admin/staff/roles", support.token(), 403);

        // Aperçu du tableau de bord logistique : pas de chiffre d'affaires
        JsonNode logistics = get("/api/admin/kpis?viewAs=DELIVERY_ADMIN", superToken, 200);
        assertTrue(logistics.get("revenue").isNull());
        assertFalse(logistics.get("deliveries").isNull());
        // Le Super Admin lui-même garde la vue complète
        assertFalse(get("/api/admin/kpis", superToken, 200).get("revenue").isNull());
        // Un autre rôle ne peut pas demander l'aperçu d'un rôle
        get("/api/admin/kpis?viewAs=ADMIN", support.token(), 403);
    }
}
