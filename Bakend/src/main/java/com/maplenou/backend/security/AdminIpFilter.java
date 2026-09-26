package com.maplenou.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Restriction d'accès à {@code /api/admin/**} par liste blanche d'IP/plages CIDR (VPN, IP fixe
 * du bureau...) — le cahier des charges demande une authentification renforcée spécifiquement
 * pour l'Admin, distincte du 2FA générique déjà en place (voir CLAUDE.md §11).
 *
 * IPv4 uniquement (couvre le cas d'usage réel : VPN d'entreprise, IP fixe de bureau — étendre à
 * IPv6 seulement si un jour un accès admin passe par un réseau IPv6). Une IP qui ne parse pas en
 * IPv4 (ex: adresse IPv6 pure) est donc toujours rejetée, jamais "par défaut autorisée" — sécurité
 * fermée par défaut. Piège classique en test local sous Windows : "localhost" résout souvent en
 * IPv6 (::1), pas en 127.0.0.1 — tester avec l'IP 127.0.0.1 explicite, jamais "localhost".
 *
 * Désactivé par défaut ({@code app.admin.allowed-ips} vide) pour ne pas bloquer le développement
 * local : en production, laisser cette variable vide équivaut à ne PAS restreindre l'accès admin,
 * donc à définir explicitement avant la mise en ligne.
 */
@Component
public class AdminIpFilter extends OncePerRequestFilter {

    private List<CidrRange> allowedRanges = Collections.emptyList();
    private Set<String> trustedProxyIps = Collections.emptySet();

    @Value("${app.admin.allowed-ips:}")
    public void setAllowedIps(String raw) {
        if (raw == null || raw.isBlank()) {
            this.allowedRanges = Collections.emptyList();
            return;
        }
        this.allowedRanges = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(CidrRange::parse)
                .collect(Collectors.toList());
    }

    @Value("${app.trusted-proxy-ips:}")
    public void setTrustedProxyIps(String raw) {
        if (raw == null || raw.isBlank()) {
            this.trustedProxyIps = Collections.emptySet();
        } else {
            this.trustedProxyIps = Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/admin/") || allowedRanges.isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        boolean allowed = allowedRanges.stream().anyMatch(range -> range.contains(clientIp));

        if (!allowed) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Accès à l'administration refusé depuis cette adresse\",\"status\":403}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Même logique que RateLimitFilter : X-Forwarded-For seulement depuis un proxy de confiance. */
    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyIps.isEmpty() && trustedProxyIps.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    /** Une IPv4 exacte ("41.207.1.2") ou une plage CIDR ("10.0.0.0/8"). */
    private static final class CidrRange {
        private final int network;
        private final int mask;

        private CidrRange(int network, int mask) {
            this.network = network;
            this.mask = mask;
        }

        static CidrRange parse(String value) {
            String[] parts = value.split("/", 2);
            int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : 32;
            int mask = prefixLength == 0 ? 0 : (int) (0xFFFFFFFFL << (32 - prefixLength));
            int network = toInt(parts[0]) & mask;
            return new CidrRange(network, mask);
        }

        boolean contains(String ip) {
            try {
                return (toInt(ip) & mask) == network;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        private static int toInt(String ip) {
            try {
                byte[] bytes = InetAddress.getByName(ip).getAddress();
                if (bytes.length != 4) {
                    throw new IllegalArgumentException("IPv6 non supporté : " + ip);
                }
                return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | (bytes[3] & 0xFF);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Adresse IP invalide : " + ip, e);
            }
        }
    }
}
