package com.maplenou.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Résout l'IP réelle d'un client, en ne faisant confiance à l'en-tête {@code X-Forwarded-For}
 * que si la requête provient d'un reverse proxy explicitement listé ({@code app.trusted-proxy-ips})
 * — sinon n'importe quel client pourrait usurper son IP avec un faux en-tête. Même logique que
 * {@link com.maplenou.backend.security.RateLimitFilter}, extraite ici pour être réutilisable
 * (ex: verrouillage de compte par IP, voir {@code AuthService}).
 */
@Component
public class ClientIpResolver {

    private final List<String> trustedProxyIps;

    public ClientIpResolver(@Value("${app.trusted-proxy-ips:}") String trustedProxyIpsCsv) {
        this.trustedProxyIps = trustedProxyIpsCsv.isBlank()
                ? List.of()
                : List.of(trustedProxyIpsCsv.split(","));
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyIps.isEmpty() && trustedProxyIps.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
