package com.maplenou.support.platform.security;

import com.maplenou.support.platform.config.MaplenouProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * HMAC (HS256) tokens signed by this service (sign-in with a Maplenou account, demo login, tests).
 * To trust another identity provider instead, leave {@code maplenou.security.jwt-secret} empty and configure
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}.
 */
@Configuration
@ConditionalOnExpression("!'${maplenou.security.jwt-secret:}'.isBlank()")
public class JwtConfig {

    private final SecretKey key;
    private final String issuer;

    public JwtConfig(MaplenouProperties props) {
        byte[] secret = props.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("maplenou.security.jwt-secret must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret, "HmacSHA256");
        this.issuer = props.security().issuer();
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> validator = issuer == null || issuer.isBlank()
                ? JwtValidators.createDefault()
                : new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer));
        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }
}
