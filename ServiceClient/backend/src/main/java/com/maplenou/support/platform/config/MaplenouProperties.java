package com.maplenou.support.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

/** All module settings under the {@code maplenou.*} prefix (see application.yml and .env.example). */
@ConfigurationProperties(prefix = "maplenou")
public record MaplenouProperties(
        Security security,
        Sla sla,
        Tickets tickets,
        Attachments attachments,
        Notifications notifications,
        Ecosystem ecosystem,
        Cors cors,
        Cache cache,
        Core core,
        Outbox outbox,
        RateLimit rateLimit,
        Assistant assistant) {

    public MaplenouProperties {
        if (tickets == null) tickets = new Tickets(null, null);
        if (outbox == null) outbox = new Outbox(null, 0);
        if (rateLimit == null) rateLimit = new RateLimit(true, 0, 0);
        if (assistant == null) assistant = new Assistant(false, null, null, null);
    }

    /**
     * Maplenou core platform (Bakend, repository folder ../Bakend).
     *
     * @param baseUrl      e.g. http://host.docker.internal:8080; used for sign-in and, with ecosystem.mode=http,
     *                     for the internal API /internal/v1/**
     * @param loginEnabled sign-in with a Maplenou account (phone + password) through POST /api/v1/auth/maplenou/*
     * @param timeout      connect and read timeout of the calls to the core
     */
    public record Core(String baseUrl, boolean loginEnabled, Duration timeout) {
        public Core {
            if (timeout == null) timeout = Duration.ofSeconds(5);
        }
    }

    /**
     * Reference-data cache (categories, vocabulary, business calendar): entries live {@code referenceTtl} and at
     * most {@code maxEntries} are kept, so memory use is bounded.
     */
    public record Cache(Duration referenceTtl, long maxEntries) {
        public Cache {
            if (referenceTtl == null) referenceTtl = Duration.ofMinutes(10);
            if (maxEntries <= 0) maxEntries = 500;
        }
    }

    public record Security(String jwtSecret, String issuer, Duration tokenTtl, boolean devLoginEnabled) {
    }

    /**
     * @param mode {@code always} (24/7, weekends and holidays count — default) or {@code business-hours}
     *             (working days between opening and closing, minus holidays)
     */
    public record Sla(String zone, LocalTime opening, LocalTime closing, String mode) {

        public boolean continuous() {
            return mode == null || mode.isBlank() || "always".equalsIgnoreCase(mode.trim());
        }
    }

    /**
     * @param autoCloseAfter resolved tickets without a reopening request are closed after this time (empty or 0 =
     *                       never, DEC-CS11-04)
     * @param autoCloseCheck how often the automatic closure runs
     */
    public record Tickets(Duration autoCloseAfter, Duration autoCloseCheck) {
        public Tickets {
            if (autoCloseCheck == null) autoCloseCheck = Duration.ofMinutes(15);
        }
    }

    /**
     * @param storage    {@code local} (private folder) or {@code cloudinary} (authenticated delivery)
     * @param antivirus  {@code none} or {@code clamav} (clamd at clamavHost:clamavPort)
     * @param failClosed with an antivirus configured: refuse uploads while it cannot be reached (default true)
     */
    public record Attachments(String storageDir, long maxFileBytes, int maxFilesPerMessage, long maxTicketBytes,
                              String storage, String cloudinaryUrl, String cloudinaryFolder, String antivirus,
                              String clamavHost, int clamavPort, Boolean failClosed, Duration scanTimeout) {
        public Attachments {
            if (storage == null || storage.isBlank()) storage = "local";
            if (antivirus == null || antivirus.isBlank()) antivirus = "none";
            if (cloudinaryFolder == null || cloudinaryFolder.isBlank()) cloudinaryFolder = "maplenou-support";
            if (clamavPort <= 0) clamavPort = 3310;
            if (failClosed == null) failClosed = Boolean.TRUE;
            if (scanTimeout == null) scanTimeout = Duration.ofSeconds(30);
        }
    }

    /**
     * @param mobileRelay also push requesters' notifications through Bakend to the Maplenou mobile apps (the devices
     *                    the apps registered with Bakend), DEC-CS11-09
     */
    public record Notifications(boolean dispatcherEnabled, Duration dispatchInterval, int maxAttempts,
                                String firebaseCredentialsFile, boolean mobileRelay, String appUrl) {
    }

    /** {@code mode}: {@code demo} (in-memory sample data) or {@code http} (Maplenou core services). */
    public record Ecosystem(String mode, String baseUrl, String serviceToken, Duration timeout) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Transactional outbox relay: polling interval for events not delivered right after commit, and retries. */
    public record Outbox(Duration pollInterval, int maxAttempts) {
        public Outbox {
            if (pollInterval == null) pollInterval = Duration.ofSeconds(5);
            if (maxAttempts <= 0) maxAttempts = 10;
        }
    }

    /**
     * Requests per minute and per caller (user, or address when anonymous); writes have their own, lower limit.
     */
    public record RateLimit(boolean enabled, int perMinute, int writesPerMinute) {
        public RateLimit {
            if (perMinute <= 0) perMinute = 300;
            if (writesPerMinute <= 0) writesPerMinute = 60;
        }
    }

    /**
     * Documentation assistant used for the AI help inside tickets (category/priority suggestion, reply drafts).
     *
     * @param url   internal address of the assistant service, e.g. http://assistant:8000
     * @param token shared secret sent as {@code X-Assistant-Token} (same value as the assistant's
     *              ASSISTANT_INTERNAL_TOKEN)
     */
    public record Assistant(boolean enabled, String url, String token, Duration timeout) {
        public Assistant {
            if (timeout == null) timeout = Duration.ofSeconds(60);
        }

        public boolean configured() {
            return enabled && url != null && !url.isBlank() && token != null && !token.isBlank();
        }
    }
}
