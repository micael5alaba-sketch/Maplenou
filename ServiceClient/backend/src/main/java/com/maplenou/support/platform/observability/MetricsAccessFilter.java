package com.maplenou.support.platform.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Opens {@code /actuator/prometheus} only to callers presenting {@code Authorization: Bearer <METRICS_TOKEN>}
 * (Prometheus {@code authorization} setting). Without a configured token the endpoint does not exist (404).
 */
public class MetricsAccessFilter extends OncePerRequestFilter {

    private final byte[] token;

    public MetricsAccessFilter(String token) {
        this.token = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (token.length == 0) {
            response.sendError(404);
            return;
        }
        String header = request.getHeader("Authorization");
        byte[] given = header != null && header.startsWith("Bearer ")
                ? header.substring(7).getBytes(StandardCharsets.UTF_8) : new byte[0];
        if (!MessageDigest.isEqual(token, given)) {
            response.sendError(401);
            return;
        }
        chain.doFilter(request, response);
    }
}
