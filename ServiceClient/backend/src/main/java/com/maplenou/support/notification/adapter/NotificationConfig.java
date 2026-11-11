package com.maplenou.support.notification.adapter;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.maplenou.support.notification.adapter.out.maplenou.BakendPushRelay;
import com.maplenou.support.notification.adapter.out.push.FirebasePushSender;
import com.maplenou.support.notification.adapter.out.push.LoggingPushSender;
import com.maplenou.support.notification.application.NotificationSettings;
import com.maplenou.support.notification.application.port.MobilePushRelay;
import com.maplenou.support.notification.application.port.PushSender;
import com.maplenou.support.platform.config.MaplenouProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Push channels: Firebase when a service-account file is configured (else log-only), and the Bakend relay. */
@Configuration
public class NotificationConfig {

    private static final Logger log = LoggerFactory.getLogger(NotificationConfig.class);

    @Bean
    NotificationSettings notificationSettings(MaplenouProperties props) {
        return new NotificationSettings(props.notifications().maxAttempts(), props.cache().referenceTtl(),
                props.notifications().appUrl());
    }

    @Bean
    @ConditionalOnMissingBean(PushSender.class)
    PushSender pushSender(MaplenouProperties props) throws IOException {
        String file = props.notifications().firebaseCredentialsFile();
        if (file == null || file.isBlank()) {
            log.info("Firebase credentials not configured: push notifications are logged only.");
            return new LoggingPushSender();
        }
        if (!Files.isReadable(Path.of(file))) {
            // fail fast with a message that says what to do (docs/guide/firebase.md)
            throw new IllegalStateException("FIREBASE_CREDENTIALS_FILE=" + file + " cannot be read. Put the Firebase "
                    + "service-account JSON in the secrets/ folder (mounted at /run/secrets/maplenou) or empty the "
                    + "variable to log push notifications only.");
        }
        try (InputStream in = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Firebase Cloud Messaging enabled (project {}).", options.getProjectId());
            return new FirebasePushSender(FirebaseMessaging.getInstance(app));
        }
    }

    @Bean
    @ConditionalOnMissingBean(MobilePushRelay.class)
    MobilePushRelay mobilePushRelay(MaplenouProperties props, RestClient.Builder builder) {
        MaplenouProperties.Ecosystem eco = props.ecosystem();
        boolean configured = props.notifications().mobileRelay() && eco != null && eco.serviceToken() != null
                && !eco.serviceToken().isBlank() && eco.baseUrl() != null && !eco.baseUrl().isBlank();
        if (!configured) {
            return new MobilePushRelay() {
                @Override
                public boolean enabled() {
                    return false;
                }

                @Override
                public void send(UUID userId, String title, String body, Map<String, String> data) {
                    // switched off: MAPLENOU_PUSH_RELAY=false or no SUPPORT_SERVICE_TOKEN
                }
            };
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(eco.timeout());
        rf.setReadTimeout(eco.timeout());
        return new BakendPushRelay(builder.clone().baseUrl(eco.baseUrl()).requestFactory(rf)
                .defaultHeader("X-Maplenou-Service-Token", eco.serviceToken()).build());
    }
}
