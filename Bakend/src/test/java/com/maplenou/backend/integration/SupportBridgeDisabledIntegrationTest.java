package com.maplenou.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sans SUPPORT_SERVICE_TOKEN, l'API interne n'existe pas (404), même avec un en-tête. */
@TestPropertySource(properties = "app.support-bridge.token=")
class SupportBridgeDisabledIntegrationTest extends AbstractIntegrationTest {

    @Test
    void internalApiIsDisabledByDefault() throws Exception {
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/internal/v1/support-tickets"))
                        .header("X-Maplenou-Service-Token", "quelconque").GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, res.statusCode());
    }
}
