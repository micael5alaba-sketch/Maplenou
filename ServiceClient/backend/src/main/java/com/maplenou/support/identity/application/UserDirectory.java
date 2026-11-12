package com.maplenou.support.identity.application;

import com.maplenou.support.identity.application.port.UserStore;
import com.maplenou.support.identity.domain.SupportUser;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Public API of the identity module: keeps the local projection of users in sync with the access tokens, tells who
 * is active, finds support staff and the names other modules show.
 */
@Service
public class UserDirectory {

    private static final Duration SYNC_INTERVAL = Duration.ofMinutes(5);
    private static final int MAX_CACHED = 10_000;

    /** What other modules may know about a person. */
    public record UserSummary(UUID id, String name, Role role, String email, String phone, boolean active,
                              String language, Instant knownSince) {
        static UserSummary of(SupportUser u) {
            return new UserSummary(u.id(), u.displayName(), u.role(), u.email(), u.phone(), u.active(), u.language(),
                    u.createdAt());
        }
    }

    private final UserStore users;
    private final Clock clock;
    private final Map<UUID, Instant> lastSync = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> activeCache = new ConcurrentHashMap<>();

    public UserDirectory(UserStore users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    /** Creates or refreshes the caller's projection; returns whether the user is active. */
    @Transactional
    public boolean sync(Actor caller) {
        Instant now = clock.instant();
        Instant previous = lastSync.get(caller.id());
        if (previous != null && previous.plus(SYNC_INTERVAL).isAfter(now) && activeCache.containsKey(caller.id())) {
            return activeCache.get(caller.id());
        }
        SupportUser user = users.find(caller.id()).orElse(null);
        if (user == null) {
            user = SupportUser.register(caller.id(), caller.displayName(), caller.email(), caller.phone(),
                    caller.role(), now);
            users.save(user);
        } else if (user.refresh(caller.displayName(), caller.email(), caller.phone(), caller.role(), now)) {
            users.save(user);
        }
        if (lastSync.size() > MAX_CACHED) {
            lastSync.clear();
            activeCache.clear();
        }
        lastSync.put(caller.id(), now);
        activeCache.put(caller.id(), user.active());
        return user.active();
    }

    /** Ensures a person exists locally (imports, other modules), without touching an existing projection. */
    @Transactional
    public UserSummary ensure(UUID id, String name, String email, String phone, Role role, Instant since) {
        return users.find(id).map(UserSummary::of).orElseGet(() -> {
            SupportUser u = SupportUser.register(id, name == null || name.isBlank() ? "Utilisateur" : name, email,
                    phone, role, since);
            users.save(u);
            return UserSummary.of(u);
        });
    }

    /** Called after an administrator changes a user's active flag. */
    public void invalidate(UUID userId) {
        lastSync.remove(userId);
        activeCache.remove(userId);
    }

    @Transactional(readOnly = true)
    public UserSummary require(UUID id) {
        return users.find(id).map(UserSummary::of).orElseThrow(() -> SupportException.notFound("Utilisateur"));
    }

    @Transactional(readOnly = true)
    public java.util.Optional<UserSummary> find(UUID id) {
        return id == null ? java.util.Optional.empty() : users.find(id).map(UserSummary::of);
    }

    /** Names of many people at once (lists, histories). */
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> summaries(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return users.findAll(ids).stream().map(UserSummary::of)
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<UserSummary> activeAssignableAgents() {
        return users.withRoles(List.of(Role.SUPPORT_AGENT, Role.SUPPORT_MANAGER), true).stream()
                .map(UserSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> activeManagers() {
        return users.withRoles(List.of(Role.SUPPORT_MANAGER), true).stream().map(UserSummary::of).toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> supportStaff() {
        return users.withRoles(List.of(Role.SUPPORT_AGENT, Role.SUPPORT_MANAGER, Role.ADMINISTRATOR), false)
                .stream().map(UserSummary::of).toList();
    }

    /** Language of the notifications of a person (fr by default). */
    @Transactional(readOnly = true)
    public String language(UUID id) {
        return users.find(id).map(SupportUser::language).orElse("fr");
    }

    @Transactional
    public UserSummary setLanguage(Actor actor, String language) {
        if (!"fr".equals(language) && !"en".equals(language)) {
            throw SupportException.badRequest("INVALID_LANGUAGE", "Langue attendue : fr ou en.", null);
        }
        SupportUser u = users.find(actor.id()).orElseThrow(() -> SupportException.notFound("Utilisateur"));
        u.setLanguage(language, clock.instant());
        users.save(u);
        return UserSummary.of(u);
    }

    /** Administrator: (de)activates a support account (BR-065). */
    @Transactional
    public UserSummary setActive(Actor admin, UUID id, boolean active) {
        if (admin.role() != Role.ADMINISTRATOR) {
            throw SupportException.forbidden("Réservé aux administrateurs.", "BR-065");
        }
        if (id.equals(admin.id())) {
            throw SupportException.businessRule("CANNOT_DEACTIVATE_SELF",
                    "Vous ne pouvez pas désactiver votre propre compte.", null);
        }
        SupportUser u = users.find(id).filter(x -> x.role() != null && x.role().isSupportStaff())
                .orElseThrow(() -> SupportException.notFound("Agent"));
        u.setActive(active, clock.instant());
        users.save(u);
        invalidate(id);
        return UserSummary.of(u);
    }
}
