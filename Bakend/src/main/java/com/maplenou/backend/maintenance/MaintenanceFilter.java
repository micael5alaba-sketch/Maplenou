package com.maplenou.backend.maintenance;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Coupe tout le trafic API quand le mode maintenance est activé (voir {@link MaintenanceService}),
 * sauf les routes nécessaires pour que l'Admin puisse se connecter et désactiver la maintenance
 * lui-même (/api/auth/**, /api/admin/**), la vérification de santé de l'infra (/actuator/health),
 * et les webhooks de paiement (/api/webhooks/**, /api/payments/**) : les laisser passer évite des
 * transactions FedaPay bloquées/incohérentes pendant l'intervention plutôt que correctement
 * réconciliées à leur arrivée.
 *
 * Ordre HIGHEST_PRECEDENCE volontaire : coupe la requête avant même le rate limiting ou
 * l'authentification JWT, pour qu'une maintenance soulage vraiment la charge serveur.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class MaintenanceFilter extends OncePerRequestFilter {

    private static final String[] EXEMPT_PREFIXES = {
            "/api/auth/",
            "/api/admin/",
            "/api/webhooks/",
            "/api/payments/",
            "/actuator/health"
    };

    private final MaintenanceService maintenanceService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : EXEMPT_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return !uri.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        if (!maintenanceService.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"%s\",\"fieldErrors\":null}",
                Instant.now(),
                escapeJson(maintenanceService.getMessage())
        ));
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
