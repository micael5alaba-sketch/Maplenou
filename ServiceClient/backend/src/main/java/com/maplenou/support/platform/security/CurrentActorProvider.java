package com.maplenou.support.platform.security;

import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Reads the caller identity from the validated JWT (inbound web adapters only). */
@Component
public class CurrentActorProvider {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_NAME = "name";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_PHONE = "phone";

    public Actor get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw SupportException.unauthenticated("UNAUTHENTICATED", "Authentification requise.", "BR-073");
        }
        return fromJwt(jwt);
    }

    public static Actor fromJwt(Jwt jwt) {
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException e) {
            throw SupportException.unauthenticated("INVALID_TOKEN", "Identité invalide dans le jeton.", "BR-004");
        }
        Role role = Role.highest(roles(jwt))
                .orElseThrow(() -> SupportException.forbidden("Aucun rôle reconnu pour cet utilisateur.", "BR-073"));
        String name = jwt.getClaimAsString(CLAIM_NAME);
        return new Actor(id, role, name == null || name.isBlank() ? "Utilisateur" : name,
                jwt.getClaimAsString(CLAIM_EMAIL), jwt.getClaimAsString(CLAIM_PHONE));
    }

    public static Collection<String> roles(Jwt jwt) {
        Object raw = jwt.getClaims().get(CLAIM_ROLES);
        if (raw instanceof Collection<?> c) {
            return c.stream().map(String::valueOf).toList();
        }
        if (raw instanceof String s) {
            return List.of(s.split("[ ,]+"));
        }
        return List.of();
    }
}
