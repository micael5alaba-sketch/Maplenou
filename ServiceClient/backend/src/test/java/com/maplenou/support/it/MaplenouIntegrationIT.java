package com.maplenou.support.it;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DEC-CS10-17 — the support service inside the Maplenou platform: sign-in with a Maplenou account, support roles
 * given by phone number, and the one-time import of the former support tickets (Bakend V46).
 */
@TestPropertySource(properties = {
        "maplenou.core.login-enabled=true",
        "maplenou.ecosystem.service-token=" + FakeMaplenouCore.SERVICE_TOKEN
})
class MaplenouIntegrationIT extends IntegrationTest {

    @DynamicPropertySource
    static void core(DynamicPropertyRegistry registry) {
        registry.add("maplenou.core.base-url", FakeMaplenouCore::baseUrl);
    }

    private ResultActions login(String phone, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/maplenou/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneNumber\":\"" + phone + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void aBuyerSignsInWithTheMaplenouAccountAndUsesTheSupportToken() throws Exception {
        var res = login("+22890000001", "Passw0rd!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(false))
                .andExpect(jsonPath("$.user.id").value(FakeMaplenouCore.BUYER_ID))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.displayName").value("Afi Mensah"));
        String token = json(res, "$.accessToken");
        assertThat(token).doesNotStartWith("core-access"); // the support service issues its own token
        // the core session opened for the check is closed again
        assertThat(FakeMaplenouCore.LOGOUTS).anyMatch(b -> b.contains("core-refresh-" + FakeMaplenouCore.BUYER_ID));
        mvc.perform(post("/api/v1/support/tickets").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("DELIVERY", "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requester.id").value(FakeMaplenouCore.BUYER_ID));
    }

    private ResultActions exchange(String coreToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/maplenou/exchange").contentType(MediaType.APPLICATION_JSON)
                .content("{\"coreAccessToken\":\"" + coreToken + "\"}"));
    }

    @Test
    void theMobileAppTradesItsMaplenouTokenForASupportToken() throws Exception {
        int logoutsBefore = FakeMaplenouCore.LOGOUTS.size();
        var res = exchange("core-access-" + FakeMaplenouCore.BUYER_ID)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.id").value(FakeMaplenouCore.BUYER_ID))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        String token = json(res, "$.accessToken");
        assertThat(token).doesNotStartWith("core-access");
        // the Maplenou session belongs to the app: it is NOT closed
        assertThat(FakeMaplenouCore.LOGOUTS).hasSize(logoutsBefore);
        mvc.perform(post("/api/v1/support/tickets").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ticketJson("DELIVERY", "HIGH")))
                .andExpect(status().isCreated());
    }

    @Test
    void theExchangeRefusesUnknownOrDisabledTokensAndMapsStaffRoles() throws Exception {
        exchange("n-importe-quoi").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CORE_TOKEN"));
        exchange("core-access-" + FakeMaplenouCore.DISABLED_ID).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        // the "Agent support" role of the Maplenou back-office is a support agent here
        exchange("core-access-" + FakeMaplenouCore.STAFF_ID).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"));
        mvc.perform(post("/api/v1/auth/maplenou/exchange").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aStaffAccountMustFinishItsFirstSignInInTheMaplenouBackOfficeFirst() throws Exception {
        // temporary password not changed yet -> refused, with a clear code
        login("+22890000010", "Passw0rd!").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
        // password changed but two-factor not set up -> refused
        login("+22890000011", "Passw0rd!").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
        // everything set up -> support agent
        login("+22890000012", "Passw0rd!").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"));
        // the Super Admin is not forced to set up two-factor (same rule as the Maplenou back office)
        login("+22890000013", "Passw0rd!").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMINISTRATOR"));
    }

    @Test
    void theSameRuleAppliesToTheTokenExchange() throws Exception {
        exchange("core-access-" + FakeMaplenouCore.NEW_STAFF_ID).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
        exchange("core-access-" + FakeMaplenouCore.NO_MFA_STAFF_ID).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"));
        exchange("core-access-" + FakeMaplenouCore.READY_STAFF_ID).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"));
        exchange("core-access-" + FakeMaplenouCore.SUPER_ADMIN_ID).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMINISTRATOR"));
    }

    @Test
    void approvedSellersAreSellersAndWrongPasswordsAreRefused() throws Exception {
        login("+22890000002", "Passw0rd!").andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("SELLER"));
        login("+22890000002", "mauvais").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        login("pas-un-numero", "x").andExpect(status().isBadRequest());
    }

    @Test
    void twoFactorAccountsNeedTheirCode() throws Exception {
        var first = login("+22890000004", "Passw0rd!").andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaRequired").value(true))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        String mfaToken = json(first, "$.mfaToken");
        mvc.perform(post("/api/v1/auth/maplenou/mfa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_MFA_CODE"));
        mvc.perform(post("/api/v1/auth/maplenou/mfa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"" + mfaToken + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DELIVERY_DRIVER"));
    }

    @Test
    void anAdministratorGivesASupportRoleByPhoneNumber() throws Exception {
        Actor admin = newUser(Role.ADMINISTRATOR);
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        putJson(manager, "/api/v1/support/admin/staff-roles", "{\"phone\":\"+22890000003\",\"role\":\"SUPPORT_AGENT\"}")
                .andExpect(status().isForbidden());
        putJson(admin, "/api/v1/support/admin/staff-roles", "{\"phone\":\"+228 90 00 00 03\",\"role\":\"SUPPORT_AGENT\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("+22890000003"));
        putJson(admin, "/api/v1/support/admin/staff-roles", "{\"phone\":\"+22890000003\",\"role\":\"CUSTOMER\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_STAFF_ROLE"));
        login("+22890000003", "Passw0rd!").andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("SUPPORT_AGENT"));
        List<String> phones = json(getAs(admin, "/api/v1/support/admin/staff-roles"), "$[*].phone");
        assertThat(phones).contains("+22890000003");
        mvc.perform(as(admin, delete("/api/v1/support/admin/staff-roles/{phone}", "+22890000003")))
                .andExpect(status().isNoContent());
        login("+22890000003", "Passw0rd!").andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void formerSupportTicketsAreImportedOnceWithTheirOldNumber() throws Exception {
        FakeMaplenouCore.legacyTickets = """
                {"items":[
                 {"id":"11111111-aaaa-4bbb-8ccc-000000000001","ticketNumber":"TKT-2026-000017",
                  "requesterId":"%s","requesterName":"Afi Mensah","requesterPhone":"+22890000001",
                  "requesterEmail":null,"requesterRole":null,"category":"Problème de livraison",
                  "subject":"Colis jamais arrivé","description":"Commande payée, rien reçu.","relatedDeliveryRef":null,
                  "photoUrl":"https://res.cloudinary.com/demo/preuve.jpg","status":"OPEN","resolutionNotes":null,
                  "resolvedAt":null,"createdAt":"2026-10-20T09:00:00Z"},
                 {"id":"11111111-aaaa-4bbb-8ccc-000000000002","ticketNumber":"TKT-2026-000018",
                  "requesterId":"%s","requesterName":"Yao Livreur","requesterPhone":"+22890000009",
                  "requesterEmail":null,"requesterRole":"DELIVERY_AGENT","category":"Problème de paiement",
                  "subject":"Gain non versé","description":"Ma course du 18/10 n'est pas payée.","relatedDeliveryRef":null,
                  "photoUrl":null,"status":"RESOLVED","resolutionNotes":"Versement effectué le 22/10.",
                  "resolvedAt":"2026-10-22T10:00:00Z","createdAt":"2026-10-21T08:00:00Z"}],
                 "page":0,"size":50,"totalItems":2,"last":true}"""
                .formatted(FakeMaplenouCore.BUYER_ID, "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c09");
        Actor admin = newUser(Role.ADMINISTRATOR);
        Actor agent = newUser(Role.SUPPORT_AGENT);

        var first = mvc.perform(as(admin, post("/api/v1/support/admin/legacy-import"))).andExpect(status().isOk());
        int imported = json(first, "$.imported");
        assertThat(imported).isBetween(0, 2); // 0 when another run of this test already imported them
        assertThat(FakeMaplenouCore.LAST_HEADERS.get("service-token")).isEqualTo(FakeMaplenouCore.SERVICE_TOKEN);
        mvc.perform(as(admin, post("/api/v1/support/admin/legacy-import")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(0))
                .andExpect(jsonPath("$.skipped").value(2));

        // the old number still finds the ticket; category and status are mapped
        List<String> ids = json(getAs(agent, "/api/v1/support/tickets?q=TKT-2026-000018&size=50"), "$.items[*].id");
        assertThat(ids).hasSize(1);
        getAs(agent, "/api/v1/support/tickets/{id}", ids.get(0))
                .andExpect(jsonPath("$.legacyNumber").value("TKT-2026-000018"))
                // resolved long ago: the 7-day automatic closure (DEC-CS11-04) may already have closed it
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.oneOf("RESOLVED", "CLOSED")))
                .andExpect(jsonPath("$.category.code").value("PAYMENT"))
                .andExpect(jsonPath("$.requester.role").value("DELIVERY_DRIVER"));
        List<String> delivery = json(getAs(agent, "/api/v1/support/tickets?q=TKT-2026-000017&size=50"), "$.items[*].id");
        getAs(agent, "/api/v1/support/tickets/{id}", delivery.get(0))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.category.code").value("DELIVERY"));

        // the old module is still used by the Flutter app (DEC-CS12-02): a status change there is noted once,
        // for the staff only, and the ticket itself is left to the agent
        String legacyBefore = FakeMaplenouCore.legacyTickets;
        try {
            FakeMaplenouCore.legacyTickets = legacyBefore
                    .replace("\"status\":\"OPEN\",\"resolutionNotes\":null",
                            "\"status\":\"RESOLVED\",\"resolutionNotes\":\"Colis livré le 24/10.\"");
            mvc.perform(as(admin, post("/api/v1/support/admin/legacy-import")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.imported").value(0))
                    .andExpect(jsonPath("$.updated").value(1));
            mvc.perform(as(admin, post("/api/v1/support/admin/legacy-import")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.updated").value(0));
            getAs(agent, "/api/v1/support/tickets/{id}", delivery.get(0)).andExpect(jsonPath("$.status").value("NEW"));
            getAs(agent, "/api/v1/support/tickets/{id}/history", delivery.get(0))
                    .andExpect(jsonPath("$[?(@.action == 'LEGACY_UPDATED')].newValue").value(org.hamcrest.Matchers.contains("RESOLVED")));
        } finally {
            FakeMaplenouCore.legacyTickets = legacyBefore;
        }

        Actor customer = newUser(Role.CUSTOMER);
        mvc.perform(as(customer, post("/api/v1/support/admin/legacy-import"))).andExpect(status().isForbidden());
    }
}
