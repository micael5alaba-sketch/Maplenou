package com.maplenou.support.platform.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Accepts {@code DATABASE_URL} in the {@code postgresql://user:password@host:port/database} form that hosting
 * platforms give (Render {@code fromDatabase.connectionString}, DEC-CS12-03) as well as the JDBC form used by Compose.
 * The URL becomes {@code spring.datasource.url}; user and password come from it unless {@code DATABASE_USERNAME} /
 * {@code DATABASE_PASSWORD} are set. Nothing changes when {@code DATABASE_URL} already starts with {@code jdbc:}.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE = "maplenouDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> props = convert(environment.getProperty("DATABASE_URL"),
                environment.getProperty("DATABASE_USERNAME"), environment.getProperty("DATABASE_PASSWORD"));
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, props));
        }
    }

    /** {@code postgres[ql]://…} → datasource properties; anything else → empty (left to application.yml). */
    static Map<String, Object> convert(String url, String username, String password) {
        Map<String, Object> props = new HashMap<>();
        if (url == null || !(url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
            return props;
        }
        URI uri = URI.create(url);
        if (uri.getHost() == null) {
            throw new IllegalStateException("DATABASE_URL has no host");
        }
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        props.put("spring.datasource.url", "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "") + uri.getRawPath() + query);
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = decode(colon < 0 ? userInfo : userInfo.substring(0, colon));
            String pass = colon < 0 ? null : decode(userInfo.substring(colon + 1));
            if (username == null || username.isBlank()) {
                props.put("spring.datasource.username", user);
            }
            if ((password == null || password.isBlank()) && pass != null) {
                props.put("spring.datasource.password", pass);
            }
        }
        return props;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // after the config files are loaded
    }
}
