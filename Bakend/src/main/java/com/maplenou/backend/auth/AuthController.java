package com.maplenou.backend.auth;

import com.maplenou.backend.auth.dto.*;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final FirebaseTokenService firebaseTokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request, httpRequest);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Envoie un code à 6 chiffres par email au compte associé à ce numéro, si éligible.
     * Réponse générique systématique (202 + même message) — voir AuthService.forgotPassword.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                   HttpServletRequest httpRequest) {
        authService.forgotPassword(request.phoneNumber(), httpRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ForgotPasswordResponse.generic());
    }

    /** Vérifie le code reçu par email et définit le nouveau mot de passe. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Jeton Firebase personnalisé pour le compte connecté — nécessaire pour
     * accéder à Firebase Realtime Database (tracking GPS livreur) sous les
     * règles de sécurité qui exigent une session Firebase authentifiée.
     */
    @GetMapping("/firebase-token")
    @SecurityRequirement(name = "bearerAuth")
    public FirebaseTokenResponse getFirebaseToken(@AuthenticationPrincipal User currentUser) {
        return new FirebaseTokenResponse(firebaseTokenService.createToken(currentUser));
    }
}
