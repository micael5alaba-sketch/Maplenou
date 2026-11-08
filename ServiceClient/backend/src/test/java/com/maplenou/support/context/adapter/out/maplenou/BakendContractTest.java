package com.maplenou.support.context.adapter.out.maplenou;

import com.maplenou.support.context.application.port.EcosystemActions.ActionContext;
import com.maplenou.support.notification.adapter.out.maplenou.BakendPushRelay;
import com.maplenou.support.shared.SupportException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The calls this service makes to Bakend's {@code /internal/v1} (Bakend/supportbridge), checked against a local fake
 * that answers like Bakend: paths, JSON field names, and how Bakend refusals are reported to the agent.
 */
class BakendContractTest {

    private HttpServer server;
    private final Map<String, String> bodies = new ConcurrentHashMap<>();
    private RestClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            String path = ex.getRequestURI().getPath();
            bodies.put(ex.getRequestMethod() + " " + path,
                    new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            int status;
            String answer;
            if (path.equals("/internal/v1/orders/CMD-1/return-requests")) {
                status = 201;
                answer = "{\"ref\":\"5b8c7a8e-0000-4000-8000-000000000001\",\"status\":\"PENDING\"}";
            } else if (path.equals("/internal/v1/orders/CMD-LATE/return-requests")) {
                status = 400;
                answer = "{\"status\":400,\"message\":\"Le délai de retour de 30 jours après livraison est dépassé\"}";
            } else if (path.startsWith("/internal/v1/deliveries/") && path.endsWith("/reschedule")) {
                status = 409;
                answer = "{\"status\":409,\"message\":\"Le livreur est en route : la course ne peut pas lui être retirée.\"}";
            } else if (path.equals("/internal/v1/orders/CMD-1")) {
                status = 200;
                answer = """
                        {"ref":"CMD-1","customerId":"7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c01","customerName":"Jean",
                         "amount":15000,"currency":"XOF","status":"PAID","subOrders":[
                         {"id":"0f0f0f0f-0000-4000-8000-000000000001","shopName":"Boutique A","status":"DELIVERED",
                          "subtotal":10000},
                         {"id":"0f0f0f0f-0000-4000-8000-000000000002","shopName":"Boutique B","status":"PREPARING",
                          "subtotal":5000}]}""";
            } else if (path.endsWith("/push")) {
                status = 202;
                answer = "";
            } else {
                status = 404;
                answer = "{\"status\":404,\"message\":\"Introuvable\"}";
            }
            byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                ex.getResponseBody().write(bytes);
            }
            ex.close();
        });
        server.start();
        client = RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static final ActionContext CTX = new ActionContext("TKT-2026-00042", "Afi", UUID.randomUUID());

    @Test
    void returnRequestSendsTheFieldsBakendValidates() {
        var result = new HttpEcosystemActions(client).requestReturn("CMD-1", null, "Produit reçu cassé", "DAMAGED", CTX);
        assertThat(result.ref()).isEqualTo("5b8c7a8e-0000-4000-8000-000000000001");
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(bodies.get("POST /internal/v1/orders/CMD-1/return-requests"))
                .contains("\"ticketNumber\":\"TKT-2026-00042\"").contains("\"agentName\":\"Afi\"")
                .contains("\"reason\":\"Produit reçu cassé\"").contains("\"productCondition\":\"DAMAGED\"");
    }

    @Test
    void bakendRefusalsReachTheAgentWithBakendsMessage() {
        var actions = new HttpEcosystemActions(client);
        assertThatThrownBy(() -> actions.requestReturn("CMD-LATE", null, "Trop tard pour ce retour", "GOOD", CTX))
                .isInstanceOfSatisfying(SupportException.class, e -> {
                    assertThat(e.code()).isEqualTo("MAPLENOU_REFUSED");
                    assertThat(e.getMessage()).contains("30 jours");
                });
        assertThatThrownBy(() -> actions.rescheduleDelivery(UUID.randomUUID().toString(), "Client absent", CTX))
                .isInstanceOfSatisfying(SupportException.class, e -> assertThat(e.getMessage()).contains("en route"));
        assertThatThrownBy(() -> actions.messageDriver(UUID.randomUUID(), "Bonjour", CTX))
                .isInstanceOfSatisfying(SupportException.class, e -> assertThat(e.code()).isEqualTo("MAPLENOU_NOT_FOUND"));
    }

    @Test
    void orderSummaryCarriesItsSubOrders() {
        var order = new HttpEcosystemGateway(client).findOrder("CMD-1").orElseThrow();
        assertThat(order.subOrders()).hasSize(2);
        assertThat(order.subOrders().get(1).shopName()).isEqualTo("Boutique B");
        assertThat(new HttpEcosystemGateway(client).findOrder("CMD-NONE")).isEmpty();
    }

    @Test
    void pushRelayPostsTitleBodyAndData() {
        UUID user = UUID.randomUUID();
        new BakendPushRelay(client).send(user, "Nouvelle réponse", "Ticket TKT-2026-00042",
                Map.of("type", "AGENT_REPLIED", "source", "support"));
        assertThat(bodies.get("POST /internal/v1/users/" + user + "/push"))
                .contains("\"title\":\"Nouvelle réponse\"").contains("\"source\":\"support\"");
    }
}
