package com.maplenou.support.attachment.adapter.out.storage;

import com.maplenou.support.attachment.application.port.BinaryStorage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Attachments on Cloudinary, uploaded as {@code authenticated} raw files (functional design §13): nobody can open
 * them with a plain URL. The backend downloads them with a short-lived signed request and streams them to the
 * authorized user, so BR-046 still applies. Configuration: {@code CLOUDINARY_URL=cloudinary://key:secret@cloud}
 * (the same account as Bakend works, files go to the {@code CLOUDINARY_FOLDER} folder).
 */
public class CloudinaryStorage implements BinaryStorage {

    private static final String RESOURCE_TYPE = "raw";
    private static final String TYPE = "authenticated";

    private final RestClient api;
    private final String apiBase;
    private final String cloud;
    private final String key;
    private final String secret;
    private final String folder;
    private final Clock clock;

    public CloudinaryStorage(RestClient.Builder builder, String cloudinaryUrl, String folder, String apiBase, Clock clock) {
        URI uri = URI.create(cloudinaryUrl.trim());
        if (!"cloudinary".equals(uri.getScheme()) || uri.getUserInfo() == null || !uri.getUserInfo().contains(":")) {
            throw new IllegalStateException("CLOUDINARY_URL must look like cloudinary://<api_key>:<api_secret>@<cloud_name>");
        }
        String[] credentials = uri.getUserInfo().split(":", 2);
        this.key = credentials[0];
        this.secret = credentials[1];
        this.cloud = uri.getHost();
        this.folder = folder;
        this.apiBase = apiBase == null || apiBase.isBlank() ? "https://api.cloudinary.com" : apiBase.replaceAll("/$", "");
        this.api = builder.clone().build();
        this.clock = clock;
    }

    @Override
    public void store(String storageKey, byte[] content, String contentType) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId(storageKey));
        params.put("type", TYPE);
        params.put("overwrite", "false");
        params.put("timestamp", Long.toString(clock.instant().getEpochSecond()));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        params.forEach(body::add);
        body.add("api_key", key);
        body.add("signature", sign(params));
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "file";
            }
        });
        try {
            api.post().uri(apiBase + "/v1_1/" + cloud + "/" + RESOURCE_TYPE + "/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new IllegalStateException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Content> load(String storageKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId(storageKey));
        params.put("type", TYPE);
        params.put("expires_at", Long.toString(clock.instant().getEpochSecond() + 300));
        params.put("timestamp", Long.toString(clock.instant().getEpochSecond()));
        String query = params.entrySet().stream().map(e -> e.getKey() + "=" + enc(e.getValue()))
                .collect(Collectors.joining("&")) + "&api_key=" + enc(key) + "&signature=" + sign(params);
        try {
            byte[] bytes = api.get().uri(URI.create(apiBase + "/v1_1/" + cloud + "/" + RESOURCE_TYPE + "/download?" + query))
                    .retrieve().body(byte[].class);
            return bytes == null ? Optional.empty() : Optional.of(() -> new ByteArrayInputStream(bytes));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new IllegalStateException("Cloudinary download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Map<String, String> params = new TreeMap<>();
        params.put("public_id", publicId(storageKey));
        params.put("type", TYPE);
        params.put("timestamp", Long.toString(clock.instant().getEpochSecond()));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        params.forEach(body::add);
        body.add("api_key", key);
        body.add("signature", sign(params));
        api.post().uri(apiBase + "/v1_1/" + cloud + "/" + RESOURCE_TYPE + "/destroy")
                .contentType(MediaType.MULTIPART_FORM_DATA).body(body).retrieve().toBodilessEntity();
    }

    @Override
    public String name() {
        return "cloudinary";
    }

    private String publicId(String storageKey) {
        return folder + "/" + storageKey;
    }

    /** Cloudinary signature: SHA-1 of the parameters sorted by name, joined with &, followed by the API secret. */
    String sign(Map<String, String> params) {
        String toSign = new TreeMap<>(params).entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1")
                    .digest((toSign + secret).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
