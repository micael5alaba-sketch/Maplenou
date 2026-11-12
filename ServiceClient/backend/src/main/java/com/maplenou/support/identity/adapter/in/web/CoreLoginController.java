package com.maplenou.support.identity.adapter.in.web;

import com.maplenou.support.identity.application.CoreLoginService;
import com.maplenou.support.shared.Actor;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Maplenou sign-in")
@ConditionalOnProperty(prefix = "maplenou.core", name = "login-enabled", havingValue = "true")
public class CoreLoginController {

    private final CoreLoginService logins;

    public CoreLoginController(CoreLoginService logins) {
        this.logins = logins;
    }

    public record LoginRequest(@NotBlank @Size(max = 20) String phoneNumber, @NotBlank @Size(max = 72) String password) {
    }

    public record MfaRequest(@NotBlank @Size(max = 2048) String mfaToken,
                             @NotBlank @Pattern(regexp = "^[0-9]{6}$") String code) {
    }

    public record ExchangeRequest(@NotBlank @Size(max = 4096) String coreAccessToken) {
    }

    /** {@code mfaRequired=true}: send the 6-digit code with the {@code mfaToken} to {@code /mfa}. */
    public record LoginResponse(boolean mfaRequired, String mfaToken, String accessToken, Instant expiresAt,
                                Actor user) {
        static LoginResponse of(CoreLoginService.LoginOutcome o) {
            return new LoginResponse(o.mfaRequired(), o.mfaToken(), o.accessToken(), o.expiresAt(), o.user());
        }
    }

    @PostMapping("/api/v1/auth/maplenou/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return LoginResponse.of(logins.login(body.phoneNumber(), body.password(), clientIp(request)));
    }

    @PostMapping("/api/v1/auth/maplenou/mfa")
    public LoginResponse mfa(@Valid @RequestBody MfaRequest body, HttpServletRequest request) {
        return LoginResponse.of(logins.verifyMfa(body.mfaToken(), body.code(), clientIp(request)));
    }

    /** The Maplenou mobile app trades its own access token for a support token (no second password prompt). */
    @PostMapping("/api/v1/auth/maplenou/exchange")
    public LoginResponse exchange(@Valid @RequestBody ExchangeRequest body, HttpServletRequest request) {
        return LoginResponse.of(logins.exchange(body.coreAccessToken(), clientIp(request)));
    }

    /** The web app (Next.js) forwards the visitor's address; the core decides whether to trust it. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
