package com.maplenou.support.identity.application;

import com.maplenou.support.identity.application.port.AccessTokenIssuer;
import com.maplenou.support.identity.application.port.CoreAccounts;
import com.maplenou.support.identity.application.port.UserStore;
import com.maplenou.support.identity.domain.MaplenouRoles;
import com.maplenou.support.shared.Actor;
import com.maplenou.support.shared.Role;
import com.maplenou.support.shared.SupportException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sign-in with a Maplenou account (phone number + password, then the 6-digit code when 2FA is on), checked by the
 * core; this service then issues its own token and closes the core session (DEC-CS10-17). Created by the identity
 * configuration only when MAPLENOU_LOGIN_ENABLED=true.
 */
public class CoreLoginService {

    /** Attempts allowed per phone number and per minute, on top of the core's own lock after 5 failures. */
    static final int MAX_ATTEMPTS_PER_MINUTE = 10;

    public record LoginOutcome(boolean mfaRequired, String mfaToken, String accessToken, Instant expiresAt,
                               Actor user) {
    }

    private final CoreAccounts core;
    private final AccessTokenIssuer issuer;
    private final StaffRoleService staff;
    private final UserStore users;
    private final Clock clock;
    private final Map<String, long[]> attempts = new ConcurrentHashMap<>();

    public CoreLoginService(CoreAccounts core, AccessTokenIssuer issuer, StaffRoleService staff, UserStore users,
                            Clock clock) {
        this.core = core;
        this.issuer = issuer;
        this.staff = staff;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LoginOutcome login(String phoneNumber, String password, String clientIp) {
        String phone = MaplenouRoles.normalizePhone(phoneNumber);
        if (phone == null) {
            throw SupportException.badRequest("INVALID_PHONE", "Numéro de téléphone invalide.", null);
        }
        throttle(phone);
        CoreAccounts.CoreLogin result = core.login(phone, password, clientIp);
        if (result.mfaRequired()) {
            return new LoginOutcome(true, result.mfaToken(), null, null, null);
        }
        return complete(result);
    }

    @Transactional(readOnly = true)
    public LoginOutcome verifyMfa(String mfaToken, String code, String clientIp) {
        throttle("mfa:" + mfaToken.hashCode());
        return complete(core.verifyMfa(mfaToken, code, clientIp));
    }

    private LoginOutcome complete(CoreAccounts.CoreLogin result) {
        CoreAccounts.CoreUser u = result.user();
        core.logout(result.refreshToken(), result.accessToken());
        if (u == null || u.id() == null) {
            throw SupportException.upstream("CORE_UNAVAILABLE", "Réponse inattendue du service de connexion Maplenou.");
        }
        Role staffRole = staff.roleOf(u.phoneNumber());
        Role role = MaplenouRoles.of(u.role(), u.sellerStatus(), staffRole);
        users.find(u.id()).filter(existing -> !existing.active()).ifPresent(x -> {
            throw new SupportException(SupportException.Kind.FORBIDDEN, "ACCOUNT_DISABLED",
                    "Ce compte support est désactivé.", null);
        });
        String name = u.fullName() == null || u.fullName().isBlank() ? "Utilisateur" : u.fullName().trim();
        Actor user = new Actor(u.id(), role, name, u.email(), u.phoneNumber());
        AccessTokenIssuer.IssuedToken token = issuer.issue(user);
        return new LoginOutcome(false, null, token.accessToken(), token.expiresAt(), user);
    }

    private void throttle(String key) {
        long minute = clock.instant().getEpochSecond() / Duration.ofMinutes(1).toSeconds();
        long[] slot = attempts.compute(key, (k, v) -> v == null || v[0] != minute ? new long[]{minute, 1}
                : new long[]{minute, v[1] + 1});
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(e -> e.getValue()[0] != minute); // bounded memory
        }
        if (slot[1] > MAX_ATTEMPTS_PER_MINUTE) {
            throw SupportException.tooManyRequests("TOO_MANY_ATTEMPTS",
                    "Trop de tentatives. Réessayez dans quelques minutes.");
        }
    }
}
