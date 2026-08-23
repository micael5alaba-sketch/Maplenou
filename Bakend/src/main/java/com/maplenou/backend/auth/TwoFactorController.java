package com.maplenou.backend.auth;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.auth.dto.AuthResponse;
import com.maplenou.backend.auth.dto.MfaLoginRequest;
import com.maplenou.backend.auth.dto.TwoFactorSetupResponse;
import com.maplenou.backend.auth.dto.VerifyTotpRequest;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
@Tag(name = "2FA TOTP")
public class TwoFactorController {

    private final TotpService totpService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Initiates 2FA setup: generates a secret and saves it to the user.
     * The user must call /activate with a valid TOTP code to actually enable 2FA.
     */
    @PostMapping("/setup")
    @SecurityRequirement(name = "bearerAuth")
    public TwoFactorSetupResponse setup(@AuthenticationPrincipal User currentUser) {
        String secret = totpService.generateSecret();
        String otpAuthUri = totpService.generateOtpAuthUri(secret, currentUser.getPhoneNumber());

        currentUser.setTotpSecret(secret);
        userRepository.save(currentUser);

        return new TwoFactorSetupResponse(secret, otpAuthUri);
    }

    /**
     * Verifies the TOTP code and enables 2FA on the account.
     */
    @PostMapping("/activate")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> activate(@AuthenticationPrincipal User currentUser,
                                          @Valid @RequestBody VerifyTotpRequest request) {
        if (currentUser.getTotpSecret() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Appelez d'abord /setup pour initialiser la 2FA");
        }
        if (!totpService.verifyCode(currentUser.getTotpSecret(), request.code())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Code TOTP invalide ou expiré");
        }

        currentUser.setTotpEnabled(true);
        userRepository.save(currentUser);

        auditService.log(currentUser.getId(), AuditAction.TWO_FA_ENABLED,
                "USER", currentUser.getId(), "2FA enabled", null);

        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the MFA challenge token + TOTP code and returns full auth tokens.
     * This endpoint is PUBLIC (no JWT required).
     */
    @PostMapping("/verify-login")
    public AuthResponse verifyLogin(@Valid @RequestBody MfaLoginRequest request) {
        return authService.verifyMfaLogin(request);
    }

    /**
     * Disables 2FA. Requires the current TOTP code to confirm.
     */
    @DeleteMapping("/disable")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal User currentUser,
                                         @Valid @RequestBody VerifyTotpRequest request) {
        if (!currentUser.isTotpEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La 2FA n'est pas activée sur ce compte");
        }
        if (!totpService.verifyCode(currentUser.getTotpSecret(), request.code())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Code TOTP invalide ou expiré");
        }

        currentUser.setTotpEnabled(false);
        currentUser.setTotpSecret(null);
        userRepository.save(currentUser);

        auditService.log(currentUser.getId(), AuditAction.TWO_FA_DISABLED,
                "USER", currentUser.getId(), "2FA disabled", null);

        return ResponseEntity.noContent().build();
    }
}
