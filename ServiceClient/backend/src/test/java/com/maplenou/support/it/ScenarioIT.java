package com.maplenou.support.it;

import com.maplenou.support.demo.DemoUsers;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end validation scenarios from the roadmap (§4 "Scénarios", §5 "Scénario complet de validation").
 */
class ScenarioIT extends IntegrationTest {

    @Test
    void customer_createsTicket_getsReply_providesInfo_resolved_closed() throws Exception {
        Actor jean = existing(DemoUsers.byId(DemoUsers.CUSTOMER_JEAN));
        Actor agent = newUser(Role.SUPPORT_AGENT);

        // CLIENT → création du ticket → catégorisation
        String id = createTicket(jean, """
                {"subject":"Commande non reçue","description":"Ma commande est marquée livrée mais je n'ai rien reçu.",
                 "categoryCode":"ORDER","priority":"HIGH","orderRef":"CMD-2026-0456"}""");
        putJson(agent, "/api/v1/support/tickets/{id}/category", "{\"categoryCode\":\"DELIVERY\"}", id)
                .andExpect(status().isOk());
        // → agent du service client
        assignSelf(agent, id);
        // → vérification de la commande et de la livraison
        getAs(agent, "/api/v1/support/tickets/{id}/context", id)
                .andExpect(jsonPath("$.order.data.status").value("SHIPPED"))
                .andExpect(jsonPath("$.delivery.data.status").value("DISPUTED"));
        postJson(agent, "/api/v1/support/tickets/{id}/internal-notes",
                "{\"body\":\"Livreur contacté, nouvelle tentative demain 9h-12h.\"}", id).andExpect(status().isCreated());
        // → réponse au client → demande d'informations
        reply(agent, id, "Confirmez-vous votre disponibilité demain entre 9 h et 12 h ?");
        setStatus(agent, id, "WAITING_FOR_CUSTOMER");
        // client fournit des informations
        reply(jean, id, "Oui, je serai disponible.");
        // → résolution → notification → fermeture
        setStatus(agent, id, "RESOLVED");
        setStatus(agent, id, "CLOSED");

        getAs(jean, "/api/v1/support/tickets/{id}", id).andExpect(jsonPath("$.status").value("CLOSED"));
        List<String> statuses = json(getAs(jean, "/api/v1/support/tickets/{id}/history", id),
                "$[?(@.action=='STATUS_CHANGED')].newValue");
        assertThat(statuses).containsExactly("ASSIGNED", "IN_PROGRESS", "WAITING_FOR_CUSTOMER", "IN_PROGRESS",
                "RESOLVED", "CLOSED");
        List<String> notified = json(getAs(jean, "/api/v1/support/notifications?size=50"),
                "$.items[?(@.ticketId=='" + id + "')].type");
        assertThat(notified).contains("TICKET_CREATED", "AGENT_REPLIED", "INFORMATION_REQUESTED", "TICKET_RESOLVED",
                "TICKET_CLOSED");
    }

    @Test
    void seller_paymentProblem_paymentVerified_resolved() throws Exception {
        Actor marie = existing(DemoUsers.byId(DemoUsers.SELLER_MARIE));
        Actor manager = newUser(Role.SUPPORT_MANAGER);
        Actor agent = newUser(Role.SUPPORT_AGENT);

        String id = createTicket(marie, """
                {"subject":"Reversement manquant","description":"Le paiement n'a pas été reversé.",
                 "categoryCode":"PAYMENT","priority":"HIGH","orderRef":"CMD-2026-0789"}""");
        putJson(manager, "/api/v1/support/tickets/{id}/assign", "{\"agentId\":\"" + agent.id() + "\"}", id)
                .andExpect(status().isOk());
        getAs(agent, "/api/v1/support/tickets/{id}/context", id)
                .andExpect(jsonPath("$.payment.data.status").value("PAID"))
                .andExpect(jsonPath("$.payment.data.payoutStatus").value("NOT_PAID_OUT"))
                .andExpect(jsonPath("$.seller.data.ref").value("VND-0042"));
        reply(agent, id, "Le reversement de 49 600 FCFA a été relancé, il arrivera sous 48 h.");
        setStatus(agent, id, "RESOLVED");
        getAs(marie, "/api/v1/support/tickets/{id}", id)
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.hasItem("REQUEST_REOPEN")));
    }

    @Test
    void driver_customerAbsent_reported_analysed_newInstruction() throws Exception {
        Actor kossi = existing(DemoUsers.byId(DemoUsers.DRIVER_KOSSI));
        Actor agent = newUser(Role.SUPPORT_AGENT);

        String id = createTicket(kossi, """
                {"subject":"Client absent","description":"Client absent, ne répond pas au téléphone.",
                 "categoryCode":"DELIVERY","deliveryRef":"LIV-2026-0912","urgent":true}""");
        getAs(agent, "/api/v1/support/tickets?requesterRole=DELIVERY_DRIVER&assignee=unassigned&q=Client absent")
                .andExpect(jsonPath("$.items[?(@.id=='" + id + "')]").isNotEmpty());
        assignSelf(agent, id);
        putJson(agent, "/api/v1/support/tickets/{id}/priority", "{\"priority\":\"HIGH\",\"reason\":\"Livraison en cours\"}", id)
                .andExpect(status().isOk());
        getAs(agent, "/api/v1/support/tickets/{id}/context", id)
                .andExpect(jsonPath("$.order.data.customerName").value("Jean K."));
        reply(agent, id, "Nouvelle instruction : laissez le colis au point relais Bè-Kpota.");
        setStatus(agent, id, "RESOLVED");
        List<String> bodies = json(getAs(kossi, "/api/v1/support/tickets/{id}/messages", id), "$[*].body");
        assertThat(bodies).anyMatch(b -> b.contains("point relais"));
    }
}
