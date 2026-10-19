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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Client HTTP minimal pour l'API transactionnelle Mailjet (v3.1/send) — pas de SDK Java officiel
 * léger, même approche que {@link com.maplenou.backend.payment.FedaPayClient} pour FedaPay.
 * Authentification HTTP Basic : clé API publique en utilisateur, clé secrète en mot de passe
 * (voir https://dev.mailjet.com/email/guides/send-api-v31/).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MailjetEmailClient {

    private static final String API_URL = "https://api.mailjet.com/v3.1/send";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.mailjet.api-key}")
    private String apiKey;

    @Value("${app.mailjet.api-secret}")
    private String apiSecret;

    @Value("${app.mailjet.sender-email}")
    private String senderEmail;

    @Value("${app.mailjet.sender-name}")
    private String senderName;

    public void sendEmail(String toEmail, String subject, String htmlContent) {
        ObjectNode from = objectMapper.createObjectNode();
        from.put("Email", senderEmail);
        from.put("Name", senderName);

        ObjectNode recipient = objectMapper.createObjectNode();
        recipient.put("Email", toEmail);

        ObjectNode message = objectMapper.createObjectNode();
        message.set("From", from);
        message.putArray("To").add(recipient);
        message.put("Subject", subject);
        message.put("HTMLPart", htmlContent);

        ObjectNode body = objectMapper.createObjectNode();
        body.putArray("Messages").add(message);

        String basicAuth = Base64.getEncoder().encodeToString(
                (apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Mailjet a répondu {} à l'envoi d'email : {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Le service d'envoi d'email a refusé la requête");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur réseau lors de l'envoi d'email via Mailjet", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Service d'envoi d'email injoignable");
        }
    }
}
