package com.maplenou.backend.common;

import io.sentry.Sentry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialise le SDK Sentry "core" manuellement (voir pom.xml : le starter Spring Boot officiel
 * ne démarre pas sur Spring Boot 4.1, trop récent pour cette version de Sentry). N'initialise
 * le SDK que si {@code app.sentry.dsn} est renseignée — sinon {@link Sentry#captureException}
 * (appelé depuis {@link com.maplenou.backend.common.exception.GlobalExceptionHandler}) reste un
 * no-op silencieux, comme n'importe quel autre service externe désactivé de ce projet
 * (Cloudinary/Brevo/FedaPay).
 */
@Configuration
@Slf4j
public class SentryConfig {

    @Value("${app.sentry.dsn}")
    private String dsn;

    @Value("${app.sentry.environment}")
    private String environment;

    @PostConstruct
    public void init() {
        if (dsn == null || dsn.isBlank()) {
            log.info("Sentry désactivé (app.sentry.dsn vide) — erreurs non remontées automatiquement");
            return;
        }
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(environment);
            // RGPD (CLAUDE.md §10) : jamais l'IP ni d'autres données personnelles par défaut.
            options.setSendDefaultPii(false);
            // Pas de suivi de performance (traces) pour l'instant — seulement les erreurs,
            // pour ne pas consommer inutilement le quota gratuit Sentry dès le départ.
            options.setTracesSampleRate(0.0);
        });
        log.info("Sentry initialisé (environnement : {})", environment);
    }
}
