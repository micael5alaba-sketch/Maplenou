package com.maplenou.support.shared;

import java.util.UUID;

/**
 * The authenticated caller of a use case, derived from the access token only (BR-004). Inbound adapters build it
 * (web: from the JWT; schedulers: {@link #system()}); application services never read the security context.
 */
public record Actor(UUID id, Role role, String displayName, String email, String phone) {

    /** Identity used by automatic jobs (auto-closure…): no user, no role. */
    public static final UUID SYSTEM_ID = new UUID(0L, 0L);

    public static Actor system() {
        return new Actor(SYSTEM_ID, null, "Maplenou", null, null);
    }

    public boolean isSystem() {
        return SYSTEM_ID.equals(id);
    }
}
