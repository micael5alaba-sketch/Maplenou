package com.maplenou.support.identity.adapter;

import com.maplenou.support.identity.adapter.in.security.UserSyncFilter;
import com.maplenou.support.identity.adapter.out.maplenou.CoreAccountClient;
import com.maplenou.support.identity.adapter.out.security.JwtAccessTokenIssuer;
import com.maplenou.support.identity.application.CoreLoginService;
import com.maplenou.support.identity.application.StaffRoleService;
import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.identity.application.port.AccessTokenIssuer;
import com.maplenou.support.identity.application.port.UserStore;
import com.maplenou.support.platform.config.MaplenouProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/** Wiring of the identity module's adapters. */
@Configuration
public class IdentityConfig {

    @Bean
    UserSyncFilter userSyncFilter(UserDirectory directory) {
        return new UserSyncFilter(directory);
    }

    /** The filter runs inside the security chain only (SecurityConfig), never as a plain servlet filter. */
    @Bean
    FilterRegistrationBean<UserSyncFilter> userSyncFilterRegistration(UserSyncFilter filter) {
        FilterRegistrationBean<UserSyncFilter> r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false);
        return r;
    }

    /** Same condition as {@code JwtConfig}: tokens are signed here only when JWT_SECRET is set. */
    @Bean
    @ConditionalOnExpression("!'${maplenou.security.jwt-secret:}'.isBlank()")
    AccessTokenIssuer accessTokenIssuer(JwtEncoder encoder, MaplenouProperties props, Clock clock) {
        return new JwtAccessTokenIssuer(encoder, props, clock);
    }

    /** Sign-in with a Maplenou account, only when {@code maplenou.core.login-enabled=true} (MAPLENOU_LOGIN_ENABLED). */
    @Bean
    @ConditionalOnProperty(prefix = "maplenou.core", name = "login-enabled", havingValue = "true")
    CoreLoginService coreLoginService(MaplenouProperties props, RestClient.Builder builder,
                                      ObjectProvider<AccessTokenIssuer> issuer, StaffRoleService staff,
                                      UserStore users, Clock clock) {
        MaplenouProperties.Core core = props.core();
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(core.timeout());
        rf.setReadTimeout(core.timeout());
        CoreAccountClient client = new CoreAccountClient(builder.clone().baseUrl(core.baseUrl()).requestFactory(rf).build());
        AccessTokenIssuer tokens = issuer.getIfAvailable(() -> {
            throw new IllegalStateException("MAPLENOU_LOGIN_ENABLED requires JWT_SECRET (support tokens)");
        });
        return new CoreLoginService(client, tokens, staff, users, clock);
    }
}
