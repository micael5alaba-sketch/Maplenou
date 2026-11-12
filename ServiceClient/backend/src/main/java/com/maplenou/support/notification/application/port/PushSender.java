package com.maplenou.support.notification.application.port;

import java.util.List;
import java.util.Map;

/** Outbound push channel (Firebase Cloud Messaging in production). */
public interface PushSender {

    record PushMessage(List<String> tokens, String title, String body, Map<String, String> data) {
    }

    /**
     * @return tokens reported as permanently invalid (to be removed)
     * @throws PushDeliveryException when the delivery failed and may be retried
     */
    List<String> send(PushMessage message) throws PushDeliveryException;

    /** "firebase" when messages really leave the server, "log-only" when they are only written to the log. */
    default String mode() {
        return "firebase";
    }

    class PushDeliveryException extends Exception {
        public PushDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
