package com.maplenou.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Vérification de la signature des webhooks FedaPay — portage exact de l'algorithme officiel
 * (`WebhookSignature.php` du SDK PHP FedaPay, lib/WebhookSignature.php sur
 * github.com/fedapay/fedapay-php, consulté le 28/09/2026), FedaPay n'ayant pas de SDK Java.
 *
 * Format de l'en-tête {@code X-FEDAPAY-SIGNATURE} : {@code t=<timestamp>,s=<hex hmac-sha256>}.
 * Chaîne signée : {@code "<timestamp>.<corps brut de la requête>"}.
 */
final class FedaPayWebhookSignature {

    private static final String SCHEME = "s";
    private static final long DEFAULT_TOLERANCE_SECONDS = 300;

    private FedaPayWebhookSignature() {
    }

    /**
     * @param payload corps brut (octets exacts) reçu de FedaPay — jamais le JSON ré-sérialisé.
     * @param header  contenu de l'en-tête X-FEDAPAY-SIGNATURE.
     * @param secret  secret webhook FedaPay (app.fedapay.webhook-secret).
     * @throws SignatureVerificationException si la signature ou l'horodatage sont invalides.
     */
    static void verify(String payload, String header, String secret) {
        long timestamp = extractTimestamp(header);
        List<String> signatures = extractSignatures(header);

        if (timestamp == -1) {
            throw new SignatureVerificationException("Horodatage introuvable dans l'en-tête de signature");
        }
        if (signatures.isEmpty()) {
            throw new SignatureVerificationException("Aucune signature de schéma 's' trouvée dans l'en-tête");
        }

        String signedPayload = timestamp + "." + payload;
        String expected = computeHmacSha256Hex(signedPayload, secret);

        boolean found = signatures.stream().anyMatch(s -> constantTimeEquals(expected, s));
        if (!found) {
            throw new SignatureVerificationException("Signature invalide pour ce contenu");
        }

        long ageSeconds = Math.abs(System.currentTimeMillis() / 1000 - timestamp);
        if (ageSeconds > DEFAULT_TOLERANCE_SECONDS) {
            throw new SignatureVerificationException("Horodatage hors de la fenêtre de tolérance (rejeu possible)");
        }
    }

    private static long extractTimestamp(String header) {
        for (String item : header.split(",")) {
            String[] parts = item.split("=", 2);
            if (parts.length == 2 && parts[0].equals("t")) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static List<String> extractSignatures(String header) {
        List<String> signatures = new ArrayList<>();
        for (String item : header.split(",")) {
            String[] parts = item.split("=", 2);
            if (parts.length == 2 && parts[0].equals(SCHEME)) {
                signatures.add(parts[1]);
            }
        }
        return signatures;
    }

    private static String computeHmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer la signature HMAC-SHA256", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    static class SignatureVerificationException extends RuntimeException {
        SignatureVerificationException(String message) {
            super(message);
        }
    }
}
