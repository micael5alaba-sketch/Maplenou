package com.maplenou.backend.email;

import com.maplenou.backend.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client HTTP minimal pour l'API transactionnelle Brevo (ex-Sendinblue) — pas de SDK Java
 * officiel léger, ce projet n'ayant aucune dépendance externe pour les appels sortants (même
 * approche que {@link com.maplenou.backend.payment.FedaPayClient} pour FedaPay).
 * Doc : https://developers.brevo.com/reference/sendtransacemail
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BrevoEmailClient {

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.brevo.api-key}")
    private String apiKey;

    @Value("${app.brevo.sender-email}")
    private String senderEmail;

    @Value("${app.brevo.sender-name}")
    private String senderName;

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        ObjectNode sender = objectMapper.createObjectNode();
        sender.put("name", senderName);
        sender.put("email", senderEmail);

        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("email", toEmail);

        ObjectNode body = objectMapper.createObjectNode();
        body.set("sender", sender);
        body.putArray("to").add(recipient);
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Brevo a répondu {} à l'envoi d'email : {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Le service d'envoi d'email a refusé la requête");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur réseau lors de l'envoi d'email via Brevo", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Service d'envoi d'email injoignable");
        }
    }
}
