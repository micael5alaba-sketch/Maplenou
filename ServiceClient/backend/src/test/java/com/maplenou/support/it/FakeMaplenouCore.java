package com.maplenou.support.it;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Stands in for the Maplenou core (Bakend): /api/auth/** and /internal/v1/support-tickets. */
final class FakeMaplenouCore {

    static final String SERVICE_TOKEN = "svc-token-for-tests-0123456789";
    static final String BUYER_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c01";
    static final String SELLER_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c02";
    static final String AGENT_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c03";
    static final String STAFF_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c05";
    static final String DISABLED_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c06";
    static final String MFA_ID = "7d0e7c3e-2b7f-4a3b-9a55-1e2f3a4b5c04";

    static final HttpServer SERVER;
    static final List<String> LOGOUTS = new CopyOnWriteArrayList<>();
    static final Map<String, String> LAST_HEADERS = new ConcurrentHashMap<>();
    static volatile String legacyTickets = "{\"items\":[],\"page\":0,\"size\":50,\"totalItems\":0,\"last\":true}";

    static {
        try {
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SERVER.createContext("/api/auth/login", ex -> {
            String body = read(ex);
            if (body.contains("\"+22890000001\"") && body.contains("\"Passw0rd!\"")) {
                send(ex, 200, login(BUYER_ID, "Afi Mensah", "+22890000001", null, null));
            } else if (body.contains("\"+22890000002\"") && body.contains("\"Passw0rd!\"")) {
                send(ex, 200, login(SELLER_ID, "Kossi Boutique", "+22890000002", null, "\"APPROVED\""));
            } else if (body.contains("\"+22890000003\"") && body.contains("\"Passw0rd!\"")) {
                send(ex, 200, login(AGENT_ID, "Esi Support", "+22890000003", null, null));
            } else if (body.contains("\"+22890000004\"")) {
                send(ex, 200, "{\"mfaRequired\":true,\"mfaToken\":\"mfa-pending-token\"}");
            } else {
                send(ex, 401, "{\"status\":401,\"message\":\"Identifiants invalides\"}");
            }
        });
        SERVER.createContext("/api/auth/2fa/verify-login", ex -> {
            String body = read(ex);
            if (body.contains("mfa-pending-token") && body.contains("\"123456\"")) {
                send(ex, 200, login(MFA_ID, "Yao Double Facteur", "+22890000004", "\"DELIVERY_AGENT\"", null));
            } else {
                send(ex, 401, "{\"status\":401}");
            }
        });
        SERVER.createContext("/api/users/me", ex -> {
            String auth = String.valueOf(ex.getRequestHeaders().getFirst("Authorization"));
            if (auth.equals("Bearer core-access-" + BUYER_ID)) {
                send(ex, 200, user(BUYER_ID, "Afi Mensah", "+22890000001", null, null));
            } else if (auth.equals("Bearer core-access-" + STAFF_ID)) {
                send(ex, 200, user(STAFF_ID, "Esi Agent Support", "+22890000005", "\"SUPPORT_AGENT\"", null));
            } else if (auth.equals("Bearer core-access-" + DISABLED_ID)) {
                send(ex, 403, "{\"status\":403}");
            } else {
                send(ex, 401, "{\"status\":401}");
            }
        });
        SERVER.createContext("/api/auth/logout", ex -> {
            LOGOUTS.add(read(ex));
            send(ex, 204, "");
        });
        SERVER.createContext("/internal/v1/support-tickets", ex -> {
            LAST_HEADERS.put("service-token", String.valueOf(ex.getRequestHeaders().getFirst("X-Maplenou-Service-Token")));
            if (!SERVICE_TOKEN.equals(ex.getRequestHeaders().getFirst("X-Maplenou-Service-Token"))) {
                send(ex, 401, "{}");
                return;
            }
            send(ex, 200, legacyTickets);
        });
        SERVER.start();
    }

    static String baseUrl() {
        return "http://127.0.0.1:" + SERVER.getAddress().getPort();
    }

    private static String user(String id, String name, String phone, String role, String sellerStatus) {
        return "{\"id\":\"" + id + "\",\"fullName\":\"" + name + "\",\"phoneNumber\":\"" + phone
                + "\",\"email\":null,\"role\":" + role + ",\"phoneVerified\":true,\"sellerStatus\":" + sellerStatus + "}";
    }

    private static String login(String id, String name, String phone, String role, String sellerStatus) {
        String user = "{\"id\":\"" + id + "\",\"fullName\":\"" + name + "\",\"phoneNumber\":\"" + phone
                + "\",\"email\":null,\"role\":" + role + ",\"phoneVerified\":true,\"sellerStatus\":" + sellerStatus + "}";
        return "{\"mfaRequired\":false,\"accessToken\":\"core-access-" + id + "\",\"refreshToken\":\"core-refresh-" + id
                + "\",\"user\":" + user + "}";
    }

    private static String read(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
        ex.close();
    }

    private FakeMaplenouCore() {
    }
}
