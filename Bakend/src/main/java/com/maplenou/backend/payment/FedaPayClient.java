package com.maplenou.backend.payment;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payment.dto.CreateTransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client HTTP minimal pour l'API FedaPay (pas de SDK Java officiel — voir
 * docs.fedapay.com/api-reference). Utilise {@link java.net.http.HttpClient} (JDK) plutôt qu'une
 * dépendance externe, ce projet n'en ayant aucune pour les appels sortants.
 *
 * Forme de réponse confirmée empiriquement en sandbox (le 28/09/2026, non documentée
 * publiquement) : chaque ressource est enveloppée sous une clé "v1/&lt;ressource&gt;" (ex.
 * {@code {"v1/transaction": {"id": 514813, "payment_url": "https://...", ...}}}) —
 * {@link #unwrap} essaie cette forme puis se rabat sur "transaction"/la racine, pour ne pas
 * casser silencieusement si FedaPay fait évoluer cette enveloppe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FedaPayClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.fedapay.secret-key}")
    private String secretKey;

    @Value("${app.fedapay.base-url}")
    private String baseUrl;

    /**
     * @return l'id de la transaction créée et l'URL de paiement, déjà incluse dans la réponse
     * de création en sandbox (pas besoin d'un second appel à /token dans ce cas).
     */
    public CreatedTransaction createTransaction(CreateTransactionRequest request) {
        JsonNode transaction = unwrap(post("/v1/transactions", request));
        JsonNode idNode = transaction.get("id");
        if (idNode == null) {
            log.error("Réponse FedaPay inattendue à la création de transaction : {}", transaction);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Réponse FedaPay invalide (transaction sans id)");
        }
        JsonNode urlNode = transaction.get("payment_url");
        return new CreatedTransaction(idNode.asLong(), urlNode != null ? urlNode.asText() : null);
    }

    /** Repli si jamais "payment_url" n'est pas inclus à la création (voir {@link #createTransaction}). */
    public String generatePaymentUrl(long transactionId) {
        JsonNode response = unwrap(post("/v1/transactions/" + transactionId + "/token", null));
        JsonNode urlNode = response.has("url") ? response.get("url") : response.get("payment_url");
        if (urlNode == null) {
            log.error("Réponse FedaPay inattendue à la génération du token : {}", response);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Réponse FedaPay invalide (pas d'URL de paiement)");
        }
        return urlNode.asText();
    }

    /** Relit l'état actuel d'une transaction directement chez FedaPay — jamais faire confiance
     * aux seules données embarquées dans le corps du webhook pour un statut de paiement. */
    public FedaPayTransactionStatus fetchTransactionStatus(long transactionId) {
        JsonNode transaction = unwrap(get("/v1/transactions/" + transactionId));
        JsonNode statusNode = transaction.get("status");
        if (statusNode == null) {
            log.error("Réponse FedaPay inattendue à la relecture de transaction {} : {}", transactionId, transaction);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Réponse FedaPay invalide (transaction sans statut)");
        }
        return new FedaPayTransactionStatus(transactionId, statusNode.asText());
    }

    public record CreatedTransaction(long id, String paymentUrl) {
    }

    private JsonNode unwrap(JsonNode response) {
        if (response.has("v1/transaction")) return response.get("v1/transaction");
        if (response.has("transaction")) return response.get("transaction");
        return response;
    }

    private JsonNode post(String path, Object body) {
        try {
            String json = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");
            builder = body != null
                    ? builder.POST(HttpRequest.BodyPublishers.ofString(json))
                    : builder.POST(HttpRequest.BodyPublishers.noBody());

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return parseOrThrow(path, response);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur réseau lors de l'appel FedaPay {}", path, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Passerelle de paiement injoignable");
        }
    }

    private JsonNode get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseOrThrow(path, response);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur réseau lors de l'appel FedaPay {}", path, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Passerelle de paiement injoignable");
        }
    }

    private JsonNode parseOrThrow(String path, HttpResponse<String> response) throws Exception {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("FedaPay a répondu {} sur {} : {}", response.statusCode(), path, response.body());
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "La passerelle de paiement a refusé la requête (" + response.statusCode() + ")");
        }
        return objectMapper.readTree(response.body());
    }
}
