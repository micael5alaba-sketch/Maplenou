package com.maplenou.support.identity.application.port;

import com.maplenou.support.shared.Actor;

import java.time.Instant;

/** Issues the support service's own access tokens. */
public interface AccessTokenIssuer {

    record IssuedToken(String accessToken, Instant expiresAt) {
    }

    IssuedToken issue(Actor user);
}
