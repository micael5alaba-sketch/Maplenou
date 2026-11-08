package com.maplenou.support.identity.domain;

import com.maplenou.support.shared.Role;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Local projection of a person known to the support service, refreshed from the access token at each request.
 * {@code active} is administered here (an administrator may deactivate a support account); {@code language}
 * chooses the language of the notifications this person receives.
 */
public final class SupportUser {

    private final UUID id;
    private String displayName;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private String language;
    private final Instant createdAt;
    private Instant updatedAt;

    public SupportUser(UUID id, String displayName, String email, String phone, Role role, boolean active,
                       String language, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.displayName = displayName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = active;
        this.language = language == null ? "fr" : language;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SupportUser register(UUID id, String displayName, String email, String phone, Role role, Instant now) {
        return new SupportUser(id, displayName, email, phone, role, true, "fr", now, now);
    }

    /** Refreshes identity attributes; never changes {@code active} (administered locally). */
    public boolean refresh(String displayName, String email, String phone, Role role, Instant now) {
        boolean changed = !Objects.equals(displayName, this.displayName) || !Objects.equals(email, this.email)
                || !Objects.equals(phone, this.phone) || role != this.role;
        if (changed) {
            this.displayName = displayName;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.updatedAt = now;
        }
        return changed;
    }

    public void setActive(boolean active, Instant now) {
        this.active = active;
        this.updatedAt = now;
    }

    public void setLanguage(String language, Instant now) {
        this.language = "en".equals(language) ? "en" : "fr";
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public Role role() {
        return role;
    }

    public boolean active() {
        return active;
    }

    public String language() {
        return language;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
