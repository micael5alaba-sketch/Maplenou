package com.maplenou.support.platform.observability;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maplenou.support.platform.config.MaplenouProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Requests per minute and per caller (the user when signed in, otherwise the client address), with a lower limit
 * for writes (POST/PUT/PATCH/DELETE). Over the limit: 429 {@code TOO_MANY_REQUESTS} with {@code Retry-After}.
 * Counters live in a bounded in-memory cache (fixed one-minute windows); with several instances each instance
 * counts on its own, which is enough to stop abuse of one instance.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Window(long minute, AtomicInteger all, AtomicInteger writes) {
    }

    private final boolean enabled;
    private final int perMinute;
    private final int writesPerMinute;
    private final Cache<String, Window> windows = Caffeine.newBuilder()
            .maximumSize(50_000).expireAfterWrite(Duration.ofMinutes(2)).build();
    private final Counter rejected;

    public RateLimitFilter(MaplenouProperties props, MeterRegistry meters) {
        this.enabled = props.rateLimit().enabled();
        this.perMinute = props.rateLimit().perMinute();
        this.writesPerMinute = props.rateLimit().writesPerMinute();
        this.rejected = Counter.builder("support.ratelimit.rejected").description("Requests refused (429)")
                .register(meters);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = callerKey(request);
        long minute = System.currentTimeMillis() / 60_000;
        Window w = windows.asMap().compute(key, (k, old) -> old == null || old.minute() != minute
                ? new Window(minute, new AtomicInteger(), new AtomicInteger()) : old);
        boolean write = !("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod())
                || "OPTIONS".equals(request.getMethod()));
        int all = w.all().incrementAndGet();
        int writes = write ? w.writes().incrementAndGet() : w.writes().get();
        if (all > perMinute || (write && writes > writesPerMinute)) {
            rejected.increment();
            long retryAfter = 60 - (System.currentTimeMillis() / 1000) % 60;
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"code\":\"TOO_MANY_REQUESTS\",\"detail\":\"Trop de requêtes. "
                    + "Réessayez dans une minute.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static String callerKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt && jwt.getSubject() != null) {
            MDC.put("userId", jwt.getSubject());
            return "u:" + jwt.getSubject();
        }
        // X-Forwarded-For is never read here (a caller could forge it to dodge the limit): behind a trusted reverse
        // proxy, server.forward-headers-strategy=native lets Tomcat put the real client address in getRemoteAddr().
        return "ip:" + request.getRemoteAddr();
    }
}
