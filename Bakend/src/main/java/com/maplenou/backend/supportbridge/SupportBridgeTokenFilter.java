package com.maplenou.backend.supportbridge;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protège l'API interne {@code /internal/v1/**} lue par le module Service client (dossier ServiceClient/).
 *
 * <ul>
 *   <li>Désactivée par défaut : sans {@code SUPPORT_SERVICE_TOKEN}, toute requête reçoit une 404 (comme si
 *       l'API n'existait pas), sur le même modèle que {@code FEDAPAY_ENABLED} ou {@code COLISSIMO_ENABLED}.</li>
 *   <li>Activée : l'en-tête {@code X-Maplenou-Service-Token} doit être égal au jeton partagé, comparé en temps
 *       constant ; sinon 401. Aucun JWT utilisateur n'est accepté ici : c'est un accès de service à service,
 *       en lecture seule.</li>
 * </ul>
 * En production, le reverse proxy ne doit pas exposer {@code /internal/} sur Internet (réseau privé uniquement).
 */
@Component
public class SupportBridgeTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Maplenou-Service-Token";
    static final String PATH_PREFIX = "/internal/v1/";

    private final byte[] expected;

    public SupportBridgeTokenFilter(@Value("${app.support-bridge.token:}") String token) {
        this.expected = token == null || token.isBlank() ? null : token.trim().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (expected == null) {
            write(response, 404, "Not Found");
            return;
        }
        String given = request.getHeader(HEADER);
        if (given == null || !MessageDigest.isEqual(expected, given.trim().getBytes(StandardCharsets.UTF_8))) {
            write(response, 401, "Jeton de service invalide");
            return;
        }
        chain.doFilter(request, response);
    }

    private static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
