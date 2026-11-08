package com.maplenou.support.notification.adapter.out.maplenou;

import com.maplenou.support.notification.application.port.MobilePushRelay;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Bakend {@code POST /internal/v1/users/{id}/push}: Bakend sends the push to the devices the Maplenou apps
 * registered (its FCM tokens). A user unknown to Bakend (404) is not an error: there is nothing to send.
 */
public class BakendPushRelay implements MobilePushRelay {

    private final RestClient client;

    public BakendPushRelay(RestClient client) {
        this.client = client;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public void send(UUID userId, String title, String body, Map<String, String> data) {
        try {
            client.post().uri("/internal/v1/users/{id}/push", userId).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("title", title, "body", body, "data", data)).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            // not a Maplenou account (demo user…): nothing to relay
        }
    }
}
