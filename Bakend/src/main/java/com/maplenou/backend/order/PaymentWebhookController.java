package com.maplenou.backend.order;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.dto.PaymentWebhookRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Endpoint webhook de paiement — appelé par la passerelle (FedaPay ou stub).
 *
 * Sécurité en place :
 *   - Header X-Webhook-Secret obligatoire (secret partagé entre backend et passerelle)
 *   - DTO typé avec @Valid (pas de Map brute)
 *   - Idempotence : si déjà PAID, ignorer silencieusement
 *
 * TODO Step 4 : remplacer le secret partagé par une vérification HMAC-SHA256
 *              avec la clé secrète FedaPay (X-FedaPay-Signature header).
 */
@RestController
@RequestMapping("/api/webhooks/payment")
@RequiredArgsConstructor
@Tag(name = "Webhooks paiement")
public class PaymentWebhookController {

    private final PaymentWebhookService webhookService;

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    /**
     * Paiement confirmé → décrémentation stock + statut PAID.
     * Header requis : X-Webhook-Secret
     */
    @PostMapping("/confirmed")
    public ResponseEntity<Void> paymentConfirmed(
            @RequestHeader("X-Webhook-Secret") String secret,
            @Valid @RequestBody PaymentWebhookRequest payload) {

        verifySecret(secret);
        webhookService.handlePaymentConfirmed(payload.orderIdAsUUID(), payload.paymentReference());
        return ResponseEntity.ok().build();
    }

    /**
     * Paiement échoué → statut PAYMENT_FAILED.
     */
    @PostMapping("/failed")
    public ResponseEntity<Void> paymentFailed(
            @RequestHeader("X-Webhook-Secret") String secret,
            @Valid @RequestBody PaymentWebhookRequest payload) {

        verifySecret(secret);
        webhookService.handlePaymentFailed(payload.orderIdAsUUID());
        return ResponseEntity.ok().build();
    }

    /** Comparaison en temps constant pour éviter les timing attacks. */
    private void verifySecret(String provided) {
        byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Secret webhook invalide");
        }
    }
}
