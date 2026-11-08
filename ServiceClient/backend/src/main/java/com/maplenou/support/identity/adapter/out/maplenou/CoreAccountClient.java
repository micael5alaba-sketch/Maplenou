package com.maplenou.support.identity.adapter.out.maplenou;

import com.maplenou.support.identity.application.port.CoreAccounts;
import com.maplenou.support.shared.SupportException;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Calls the public authentication API of the Maplenou core (Bakend, {@code /api/auth/**}). */
public class CoreAccountClient implements CoreAccounts {

    private final RestClient client;

    public CoreAccountClient(RestClient client) {
        this.client = client;
    }

    @Override
    public CoreLogin login(String phoneNumber, String password, String clientIp) {
        return post("/api/auth/login", Map.of("phoneNumber", phoneNumber, "password", password), clientIp, false);
    }

    @Override
    public CoreLogin verifyMfa(String mfaToken, String code, String clientIp) {
        return post("/api/auth/2fa/verify-login", Map.of("mfaToken", mfaToken, "code", code), clientIp, true);
    }

    @Override
    public void logout(String refreshToken, String accessToken) {
        if (refreshToken == null) {
            return;
        }
        try {
            client.post().uri("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refreshToken", refreshToken, "accessToken", accessToken == null ? "" : accessToken))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException ignored) {
            // the core session simply expires
        }
    }

    private CoreLogin post(String path, Map<String, String> body, String clientIp, boolean mfa) {
        try {
            RestClient.RequestBodySpec req = client.post().uri(path).contentType(MediaType.APPLICATION_JSON);
            if (clientIp != null && !clientIp.isBlank()) {
                req = req.header("X-Forwarded-For", clientIp);
            }
            CoreLogin result = req.body(body).retrieve().body(CoreLogin.class);
            if (result == null) {
                throw unavailable();
            }
            return result;
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                throw SupportException.tooManyRequests("TOO_MANY_ATTEMPTS",
                        "Trop de tentatives. Réessayez dans quelques minutes.");
            }
            if (status == 403) {
                throw new SupportException(SupportException.Kind.FORBIDDEN, "ACCOUNT_DISABLED",
                        "Ce compte Maplenou est désactivé.", null);
            }
            if (mfa) {
                throw SupportException.unauthenticated("INVALID_MFA_CODE", "Code de vérification incorrect ou expiré.",
                        null);
            }
            throw SupportException.unauthenticated("INVALID_CREDENTIALS",
                    "Numéro de téléphone ou mot de passe incorrect.", null);
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static SupportException unavailable() {
        return SupportException.unavailable("CORE_UNAVAILABLE",
                "Le service de connexion Maplenou est momentanément indisponible. Réessayez.");
    }
}
