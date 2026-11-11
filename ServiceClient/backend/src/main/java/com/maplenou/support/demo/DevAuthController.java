package com.maplenou.support.demo;

import com.maplenou.support.identity.application.port.AccessTokenIssuer;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.SupportException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Demo login: issues a token for one of the demo personas. Only active with
 * {@code maplenou.security.dev-login-enabled=true} (demo profile) — never enable in production.
 */
@RestController
@Tag(name = "Demo login")
@ConditionalOnProperty(prefix = "maplenou.security", name = "dev-login-enabled", havingValue = "true")
public class DevAuthController {

    private final AccessTokenIssuer issuer;

    public DevAuthController(ObjectProvider<AccessTokenIssuer> issuer) {
        this.issuer = issuer.getIfAvailable(() -> {
            throw new IllegalStateException("dev-login-enabled requires maplenou.security.jwt-secret");
        });
    }

    @GetMapping("/api/v1/dev/users")
    public List<Actor> users() {
        return DemoUsers.ALL;
    }

    public record TokenRequest(@NotNull UUID userId) {
    }

    public record TokenResponse(String accessToken, Instant expiresAt, Actor user) {
    }

    @PostMapping("/api/v1/dev/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest req) {
        Actor user = DemoUsers.byId(req.userId());
        if (user == null) {
            throw SupportException.notFound("Utilisateur de démonstration");
        }
        AccessTokenIssuer.IssuedToken t = issuer.issue(user);
        return new TokenResponse(t.accessToken(), t.expiresAt(), user);
    }
}
