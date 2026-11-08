package com.maplenou.support.identity.application.port;

import java.util.UUID;

/**
 * The authentication API of the Maplenou core (Bakend {@code /api/auth/**}). The password only passes through; it
 * is never stored or logged. Errors are reported as SupportException (INVALID_CREDENTIALS, INVALID_MFA_CODE,
 * ACCOUNT_DISABLED, TOO_MANY_ATTEMPTS, CORE_UNAVAILABLE).
 */
public interface CoreAccounts {

    /** Subset of Bakend {@code UserResponse}. */
    record CoreUser(UUID id, String fullName, String phoneNumber, String email, String role, String sellerStatus) {
    }

    /** Bakend {@code LoginResponse} / {@code AuthResponse}. */
    record CoreLogin(boolean mfaRequired, String mfaToken, String accessToken, String refreshToken, CoreUser user) {
    }

    CoreLogin login(String phoneNumber, String password, String clientIp);

    CoreLogin verifyMfa(String mfaToken, String code, String clientIp);

    /** Ends the core session opened by the sign-in: the support service keeps its own token only. Best effort. */
    void logout(String refreshToken, String accessToken);
}
