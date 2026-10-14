package com.maplenou.backend.payment;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.order.Order;
import com.maplenou.backend.order.OrderRepository;
import com.maplenou.backend.order.PaymentWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Vrai webhook FedaPay (remplace le TODO historique de {@link com.maplenou.backend.order.PaymentWebhookController}).
 *
 * Sécurité : signature HMAC-SHA256 vérifiée sur le corps brut exact (voir
 * {@link FedaPayWebhookSignature}, portée du SDK PHP officiel FedaPay). Le contenu du webhook
 * n'est ensuite utilisé QUE pour savoir *quelle* transaction relire — jamais pour décider du
 * statut de paiement directement : {@link FedaPayClient#fetchTransactionStatus} revérifie l'état
 * réel auprès de FedaPay avant de toucher à la commande, au cas où le format exact du corps du
 * webhook évoluerait sans préavis (documentation publique incomplète sur ce point).
 */
@RestController
@RequestMapping("/api/webhooks/fedapay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks paiement")
public class FedaPayWebhookController {

    private final FedaPayClient fedaPayClient;
    private final OrderRepository orderRepository;
    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;

    @Value("${app.fedapay.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-FEDAPAY-SIGNATURE") String signatureHeader,
            @RequestBody String rawBody) {

        try {
            FedaPayWebhookSignature.verify(rawBody, signatureHeader, webhookSecret);
        } catch (FedaPayWebhookSignature.SignatureVerificationException e) {
            log.warn("Webhook FedaPay rejeté : {}", e.getMessage());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Signature webhook invalide");
        }

        long transactionId = extractTransactionId(rawBody);

        // Ne jamais faire confiance au statut embarqué dans le corps du webhook : on relit
        // l'état réel de la transaction directement chez FedaPay avant de toucher à la commande.
        FedaPayTransactionStatus status = fedaPayClient.fetchTransactionStatus(transactionId);

        Order order = orderRepository.findByFedapayTransactionId(transactionId).orElse(null);
        if (order == null) {
            // Transaction FedaPay inconnue de ce backend (test manuel dans le dashboard,
            // transaction orpheline...) : on répond 200 pour que FedaPay arrête de réessayer,
            // sans rien modifier.
            log.warn("Webhook FedaPay reçu pour une transaction {} sans commande associée", transactionId);
            return ResponseEntity.ok().build();
        }

        if (status.isPaid()) {
            paymentWebhookService.handlePaymentConfirmed(order.getId(), String.valueOf(transactionId));
        } else if (status.isFailed()) {
            paymentWebhookService.handlePaymentFailed(order.getId());
        } else {
            log.info("Webhook FedaPay pour la commande {} : statut '{}' non terminal, ignoré",
                    order.getOrderNumber(), status.status());
        }

        return ResponseEntity.ok().build();
    }

    /** L'id de transaction se trouve soit sous "entity.id", soit sous "object_id" à la racine
     * selon la forme exacte de l'événement (voir Event.php du SDK PHP officiel). */
    private long extractTransactionId(String rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode entity = root.get("entity");
            if (entity != null && entity.has("id")) {
                return entity.get("id").asLong();
            }
            JsonNode objectId = root.get("object_id");
            if (objectId != null) {
                return objectId.asLong();
            }
            throw new IllegalArgumentException("Ni entity.id ni object_id trouvés");
        } catch (Exception e) {
            log.error("Corps de webhook FedaPay illisible : {}", rawBody, e);
            throw new ApiException(HttpStatus.BAD_REQUEST, "Corps de webhook invalide");
        }
    }
}
