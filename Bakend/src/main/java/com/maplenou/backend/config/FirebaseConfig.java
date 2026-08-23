package com.maplenou.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.service-account-path}")
    private Resource serviceAccountResource;

    @Value("${app.firebase.database-url}")
    private String databaseUrl;

    @PostConstruct
    public void initialize() {
        // Éviter la double initialisation lors du rechargement DevTools
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            GoogleCredentials credentials = GoogleCredentials
                    .fromStream(serviceAccountResource.getInputStream());

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setDatabaseUrl(databaseUrl)
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialisé (project: {})", databaseUrl);

        } catch (Exception e) {
            // En dev, si le fichier placeholder est présent (clé invalide, JSON malformé, etc.),
            // on log un avertissement mais on ne bloque PAS le démarrage.
            // En production, remplacez firebase-service-account.json par le vrai fichier.
            log.warn("Firebase non initialisé — notifications push désactivées. Cause : {}", e.getMessage());
        }
    }
}
