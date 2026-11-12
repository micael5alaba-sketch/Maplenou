package com.maplenou.support.identity.adapter.out.security;

import com.maplenou.support.identity.application.port.AccessTokenIssuer;
import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.Actor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Issues HS256 access tokens signed with JWT_SECRET (Maplenou sign-in, demo login, tests). */
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder encoder, MaplenouProperties props, Clock clock) {
        this.encoder = encoder;
        this.issuer = props.security().issuer();
        this.ttl = props.security().tokenTtl() == null ? Duration.ofHours(8) : props.security().tokenTtl();
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(Actor user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(ttl);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(user.id().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CurrentActorProvider.CLAIM_ROLES, List.of(user.role().name()))
                .claim(CurrentActorProvider.CLAIM_NAME, user.displayName());
        if (issuer != null && !issuer.isBlank()) {
            claims.issuer(issuer);
        }
        if (user.email() != null) {
            claims.claim(CurrentActorProvider.CLAIM_EMAIL, user.email());
        }
        if (user.phone() != null) {
            claims.claim(CurrentActorProvider.CLAIM_PHONE, user.phone());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
