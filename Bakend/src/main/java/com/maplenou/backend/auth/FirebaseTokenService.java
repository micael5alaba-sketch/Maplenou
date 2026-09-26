package com.maplenou.backend.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Émet un jeton Firebase personnalisé pour le compte connecté, pour que le
 * client Flutter puisse s'authentifier auprès de Firebase Realtime Database
 * (tracking GPS livreur, voir `notification/FirebaseRealtimeDbStructure.java`)
 * sans dupliquer un système de compte séparé — l'UID Firebase est l'UUID du
 * compte, et la revendication `role` reflète le rôle `DELIVERY_AGENT` pour
 * que les règles de sécurité RTDB puissent restreindre l'écriture au
 * livreur assigné.
 */
@Slf4j
@Service
public class FirebaseTokenService {

    public String createToken(User user) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Firebase n'est pas configuré sur ce serveur");
        }

        Map<String, Object> claims = user.getRole() == Role.DELIVERY_AGENT
                ? Map.of("role", "DELIVERY_AGENT")
                : Map.of();

        try {
            return FirebaseAuth.getInstance().createCustomToken(user.getId().toString(), claims);
        } catch (Exception e) {
            log.error("Échec de la génération du jeton Firebase pour {}", user.getId(), e);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Impossible de générer le jeton Firebase pour le moment");
        }
    }
}
