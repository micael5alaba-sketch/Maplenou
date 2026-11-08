package com.maplenou.support.platform.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start a publicly hosted backend with unsafe settings (DEC-CS12-03) — the counterpart, for hosting
 * platforms, of the Compose {@code public} profile's guard ({@code deploy/guard.sh}). Active when Render's own
 * {@code RENDER=true} variable or {@code PUBLIC_DEPLOYMENT=true} is set.
 */
@Component
public class PublicDeploymentGuard {

    public PublicDeploymentGuard(Environment env, MaplenouProperties props) {
        List<String> problems = check(env, props);
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Public deployment refused: " + String.join("; ", problems));
        }
    }

    static List<String> check(Environment env, MaplenouProperties props) {
        List<String> problems = new ArrayList<>();
        boolean isPublic = "true".equalsIgnoreCase(env.getProperty("RENDER"))
                || "true".equalsIgnoreCase(env.getProperty("PUBLIC_DEPLOYMENT"));
        if (!isPublic) {
            return problems;
        }
        if (env.acceptsProfiles(Profiles.of("demo"))) {
            problems.add("the demo profile (demo login, fake data) must never be public — set SPRING_PROFILES_ACTIVE=default");
        }
        String secret = props.security() == null ? null : props.security().jwtSecret();
        if (secret == null || secret.length() < 32 || secret.startsWith("demo-only")) {
            problems.add("JWT_SECRET must be a random value of at least 32 characters");
        }
        return problems;
    }
}
