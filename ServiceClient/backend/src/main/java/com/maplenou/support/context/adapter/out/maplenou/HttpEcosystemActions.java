package com.maplenou.support.context.adapter.out.maplenou;

import com.maplenou.support.context.application.port.EcosystemActions;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Asks Bakend to act ({@code POST /internal/v1/...}, supportbridge), DEC-CS11-07. */
public class HttpEcosystemActions implements EcosystemActions {

    private final RestClient client;

    public HttpEcosystemActions(RestClient client) {
        this.client = client;
    }

    @Override
    public ActionResult requestReturn(String orderRef, String subOrderId, String reason, String productCondition,
                                      ActionContext ctx) {
        Map<String, Object> body = base(ctx);
        body.put("subOrderId", subOrderId);
        body.put("reason", reason);
        body.put("productCondition", productCondition);
        return post("/internal/v1/orders/{ref}/return-requests", body, orderRef);
    }

    @Override
    public ActionResult rescheduleDelivery(String deliveryRef, String reason, ActionContext ctx) {
        Map<String, Object> body = base(ctx);
        body.put("reason", reason);
        return post("/internal/v1/deliveries/{ref}/reschedule", body, deliveryRef);
    }

    @Override
    public ActionResult messageDriver(UUID driverId, String message, ActionContext ctx) {
        Map<String, Object> body = base(ctx);
        body.put("message", message);
        return post("/internal/v1/drivers/{id}/support-messages", body, driverId);
    }

    private static Map<String, Object> base(ActionContext ctx) {
        Map<String, Object> body = new HashMap<>();
        body.put("ticketNumber", ctx.ticketNumber());
        body.put("agentName", ctx.agentName());
        body.put("agentId", ctx.agentId() == null ? null : ctx.agentId().toString());
        return body;
    }

    private ActionResult post(String path, Map<String, Object> body, Object ref) {
        try {
            ActionResult result = client.post().uri(path, ref).contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(ActionResult.class);
            return result == null ? new ActionResult(null, "DONE") : result;
        } catch (RestClientException e) {
            throw BakendClient.translate(e);
        }
    }
}
