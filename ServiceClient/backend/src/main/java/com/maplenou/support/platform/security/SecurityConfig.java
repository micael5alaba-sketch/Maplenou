package com.maplenou.support.platform.security;

import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.platform.observability.MetricsAccessFilter;
import com.maplenou.support.platform.observability.RateLimitFilter;
import com.maplenou.support.shared.Role;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;

/**
 * Deny-by-default security (BR-073..BR-076, OWASP Authorization Cheat Sheet): every API call needs a valid bearer
 * token; role checks are done per endpoint and resource-level checks by each module's access policy.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Metrics for Prometheus: a separate chain, opened only with the METRICS_TOKEN (404 when it is empty). */
    @Bean
    @Order(1)
    SecurityFilterChain metrics(HttpSecurity http, MaplenouProperties props,
                                org.springframework.core.env.Environment env) throws Exception {
        http.securityMatcher("/actuator/prometheus")
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new MetricsAccessFilter(env.getProperty("maplenou.metrics-token", "")),
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain api(HttpSecurity http, MaplenouProperties props,
                            ObjectProvider<AuthenticatedRequestFilter> moduleFilters,
                            RateLimitFilter rateLimit) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies
                .cors(cors -> cors.configurationSource(corsSource(props)))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h
                        .contentTypeOptions(c -> { })
                        .frameOptions(f -> f.deny())
                        .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/dev/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/dev/token").permitAll()
                        // sign-in with a Maplenou account (only exists when MAPLENOU_LOGIN_ENABLED=true)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/maplenou/login", "/api/v1/auth/maplenou/mfa").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(rateLimit, BearerTokenAuthenticationFilter.class);
        for (AuthenticatedRequestFilter f : moduleFilters.orderedStream().toList()) {
            http.addFilterAfter(f, RateLimitFilter.class);
        }
        return http.build();
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false); // added to the security chain above
        return r;
    }

    /** Maps the {@code roles} claim to {@code ROLE_*} authorities. */
    static Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            Collection<String> roles = CurrentActorProvider.roles(jwt);
            List<GrantedAuthority> authorities = roles.stream()
                    .filter(r -> {
                        try {
                            Role.valueOf(r);
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private static CorsConfigurationSource corsSource(MaplenouProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.cors() == null || props.cors().allowedOrigins() == null
                ? List.of() : props.cors().allowedOrigins();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "If-Match", "X-Request-Id"));
        cfg.setExposedHeaders(List.of("Content-Disposition", "X-Request-Id", "Retry-After"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
