package com.maplenou.support.platform.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void renderConnectionStringBecomesAJdbcUrlWithItsCredentials() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.convert(
                "postgresql://support:p%40ss%3Aw0rd@dpg-abc123-a:5432/maplenou_support?sslmode=prefer", null, null);
        assertThat(p).containsEntry("spring.datasource.url",
                        "jdbc:postgresql://dpg-abc123-a:5432/maplenou_support?sslmode=prefer")
                .containsEntry("spring.datasource.username", "support")
                .containsEntry("spring.datasource.password", "p@ss:w0rd");
    }

    @Test
    void explicitCredentialsWinAndTheDefaultPortMayBeOmitted() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.convert(
                "postgres://u:x@db.example.org/support", "admin", "secret");
        assertThat(p).containsEntry("spring.datasource.url", "jdbc:postgresql://db.example.org/support")
                .doesNotContainKeys("spring.datasource.username", "spring.datasource.password");
    }

    @Test
    void jdbcUrlsAndMissingValuesAreLeftAlone() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.convert("jdbc:postgresql://localhost:5432/x", null, null)).isEmpty();
        assertThat(DatabaseUrlEnvironmentPostProcessor.convert(null, null, null)).isEmpty();
    }
}
