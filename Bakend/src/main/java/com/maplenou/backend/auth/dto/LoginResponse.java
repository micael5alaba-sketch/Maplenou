package com.maplenou.backend.auth.dto;

import com.maplenou.backend.user.dto.UserResponse;

public record LoginResponse(
        boolean mfaRequired,
        String mfaToken,        // only if mfaRequired=true (short-lived)
        String accessToken,     // only if mfaRequired=false
        String refreshToken,    // only if mfaRequired=false
        UserResponse user       // only if mfaRequired=false
) {
    public static LoginResponse fullAuth(String access, String refresh, UserResponse user) {
        return new LoginResponse(false, null, access, refresh, user);
    }

    public static LoginResponse mfaChallenge(String mfaToken) {
        return new LoginResponse(true, mfaToken, null, null, null);
    }
}
