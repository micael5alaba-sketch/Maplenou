package com.maplenou.backend.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Envoie une notification push à tous les appareils d'un utilisateur.
     * Les erreurs FCM sont loguées mais ne font pas échouer la transaction appelante.
     *
     * @param userId      Destinataire
     * @param title       Titre de la notification
     * @param body        Corps du message
     * @param data        Données supplémentaires (ex: orderId, type d'événement)
     */
    @Transactional
    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        if (!isFirebaseAvailable()) return;

        List<String> tokens = deviceTokenRepository.findByUserId(userId)
                .stream()
                .map(DeviceToken::getToken)
                .toList();

        if (tokens.isEmpty()) return;

        sendToTokens(tokens, title, body, data);
    }

    /**
     * Envoie une notification à une liste de tokens FCM.
     * Utilise MulticastMessage (jusqu'à 500 tokens par appel).
     */
    private void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        // FCM limite à 500 tokens par MulticastMessage
        int batchSize = 500;
        for (int i = 0; i < tokens.size(); i += batchSize) {
            List<String> batch = tokens.subList(i, Math.min(i + batchSize, tokens.size()));

            MulticastMessage.Builder msgBuilder = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .addAllTokens(batch);

            if (data != null && !data.isEmpty()) {
                msgBuilder.putAllData(data);
            }

            try {
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(msgBuilder.build());
                log.debug("FCM envoyé — succès: {}, échecs: {}",
                        response.getSuccessCount(), response.getFailureCount());

                // Nettoyer les tokens invalides (enregistrement supprimé côté Firebase)
                cleanInvalidTokens(batch, response);

            } catch (FirebaseMessagingException e) {
                log.error("Erreur FCM lors de l'envoi à {} tokens : {}", batch.size(), e.getMessage());
            }
        }
    }

    /** Supprime de la DB les tokens que Firebase signale comme invalides. */
    private void cleanInvalidTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sr = responses.get(i);
            if (!sr.isSuccessful()) {
                MessagingErrorCode errorCode = sr.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    String invalidToken = tokens.get(i);
                    deviceTokenRepository.findByToken(invalidToken)
                            .ifPresent(dt -> deviceTokenRepository.deleteByTokenAndUserId(
                                    invalidToken, dt.getUser().getId()));
                    log.debug("Token FCM invalide supprimé : {}…", invalidToken.substring(0, Math.min(20, invalidToken.length())));
                }
            }
        }
    }

    private boolean isFirebaseAvailable() {
        try {
            return !FirebaseApp.getApps().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
