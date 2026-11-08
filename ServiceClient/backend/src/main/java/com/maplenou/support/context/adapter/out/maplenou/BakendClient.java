package com.maplenou.support.context.adapter.out.maplenou;

import com.maplenou.support.shared.SupportException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Error translation shared by the adapters that call Bakend's internal API. */
final class BakendClient {

    /** Header checked by Bakend ({@code supportbridge.SupportBridgeTokenFilter}) on /internal/v1/**. */
    static final String SERVICE_TOKEN_HEADER = "X-Maplenou-Service-Token";

    private BakendClient() {
    }

    static boolean isNotFound(HttpClientErrorException e) {
        return e.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404));
    }

    /** A refusal by Maplenou's own rules (4xx with a message) or an unreachable / failing Maplenou. */
    static SupportException translate(RestClientException e) {
        if (e instanceof RestClientResponseException r && r.getStatusCode().is4xxClientError()
                && r.getStatusCode().value() != 401 && r.getStatusCode().value() != 404) {
            String body = r.getResponseBodyAsString();
            String message = extractMessage(body);
            return SupportException.businessRule("MAPLENOU_REFUSED",
                    message == null ? "Maplenou a refusé l'action." : "Maplenou a refusé l'action : " + message, null);
        }
        if (e instanceof RestClientResponseException r && r.getStatusCode().value() == 404) {
            return SupportException.businessRule("MAPLENOU_NOT_FOUND",
                    "Maplenou ne trouve pas l'objet visé (commande, livraison ou livreur).", null);
        }
        return SupportException.unavailable("ECOSYSTEM_UNAVAILABLE",
                "La plateforme Maplenou ne répond pas (vérifiez MAPLENOU_CORE_URL et SUPPORT_SERVICE_TOKEN).");
    }

    /** Bakend errors look like {"message": "..."} (or problem details with "detail"). */
    static String extractMessage(String body) {
        if (body == null) {
            return null;
        }
        for (String field : new String[]{"\"message\"", "\"detail\""}) {
            int i = body.indexOf(field);
            if (i >= 0) {
                int colon = body.indexOf(':', i);
                int start = body.indexOf('"', colon + 1);
                int end = body.indexOf('"', start + 1);
                if (colon > 0 && start > 0 && end > start) {
                    return body.substring(start + 1, end);
                }
            }
        }
        return null;
    }
}
