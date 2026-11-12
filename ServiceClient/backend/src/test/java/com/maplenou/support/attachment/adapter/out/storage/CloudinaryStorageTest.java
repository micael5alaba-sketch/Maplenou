package com.maplenou.support.attachment.adapter.out.storage;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Signed, authenticated Cloudinary storage against a local fake of the Cloudinary API. */
class CloudinaryStorageTest {

    private record Call(String method, String path, String query, String body) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private CloudinaryStorage storage;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            calls.add(new Call(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getRawQuery(), body));
            byte[] answer;
            int status = 200;
            if (exchange.getRequestURI().getPath().endsWith("/download")) {
                boolean missing = exchange.getRequestURI().getRawQuery().contains("missing");
                status = missing ? 404 : 200;
                answer = missing ? "{}".getBytes() : "PDF-BYTES".getBytes();
            } else {
                answer = "{\"result\":\"ok\"}".getBytes();
            }
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();
        Clock clock = Clock.fixed(Instant.ofEpochSecond(1_800_000_000L), ZoneOffset.UTC);
        storage = new CloudinaryStorage(RestClient.builder(), "cloudinary://123456:topsecret@demo-cloud", "maplenou-support",
                "http://127.0.0.1:" + server.getAddress().getPort(), clock);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void signatureFollowsTheCloudinaryAlgorithm() {
        // example of the Cloudinary "authentication signatures" documentation (secret "abcd")
        CloudinaryStorage doc = new CloudinaryStorage(RestClient.builder(), "cloudinary://1:abcd@x", "f", null,
                Clock.systemUTC());
        assertThat(doc.sign(Map.of("eager", "w_400,h_300,c_pad|w_260,h_200,c_crop", "public_id", "sample_image",
                "timestamp", "1315060510")))
                .isEqualTo("bfd09f95f331f558cbd1320e67aa8d488770583e");
    }

    @Test
    void uploadIsSignedAuthenticatedAndNeverSendsTheSecret() {
        storage.store("2026/10/abc.pdf", "PDF-BYTES".getBytes(), "application/pdf");
        Call upload = calls.getFirst();
        assertThat(upload.method()).isEqualTo("POST");
        assertThat(upload.path()).isEqualTo("/v1_1/demo-cloud/raw/upload");
        assertThat(upload.body()).contains("maplenou-support/2026/10/abc.pdf").contains("authenticated")
                .contains("123456").contains("1800000000").contains("PDF-BYTES").contains("signature")
                .doesNotContain("topsecret");
    }

    @Test
    void downloadUsesAShortLivedSignedUrlAndMissingFilesAreEmpty() throws IOException {
        var content = storage.load("2026/10/abc.pdf").orElseThrow();
        try (var in = content.open()) {
            assertThat(new String(in.readAllBytes())).isEqualTo("PDF-BYTES");
        }
        Call download = calls.getFirst();
        assertThat(download.path()).isEqualTo("/v1_1/demo-cloud/raw/download");
        assertThat(download.query()).contains("expires_at=1800000300").contains("api_key=123456")
                .contains("signature=").doesNotContain("topsecret");
        assertThat(storage.load("missing.pdf")).isEmpty();
    }

    @Test
    void deleteDestroysTheFileAndBadUrlsFailFast() {
        storage.delete("2026/10/abc.pdf");
        assertThat(calls.getFirst().path()).isEqualTo("/v1_1/demo-cloud/raw/destroy");
        assertThatThrownBy(() -> new CloudinaryStorage(RestClient.builder(), "https://demo-cloud", "f", null,
                Clock.systemUTC())).isInstanceOf(IllegalStateException.class).hasMessageContaining("CLOUDINARY_URL");
    }
}
