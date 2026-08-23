package com.maplenou.backend.notification;

import com.maplenou.backend.notification.dto.RegisterTokenRequest;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "Gestion des tokens FCM pour les notifications push")
@RestController
@RequestMapping("/api/notifications/tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @Operation(summary = "Enregistrer un token FCM (appelé à la connexion ou au démarrage de l'app)")
    @PostMapping
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterTokenRequest request) {
        deviceTokenService.register(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Supprimer un token FCM (déconnexion d'un appareil)")
    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal User user,
            @PathVariable String token) {
        deviceTokenService.unregister(user, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Supprimer tous les tokens FCM (déconnexion globale)")
    @DeleteMapping
    public ResponseEntity<Void> unregisterAll(@AuthenticationPrincipal User user) {
        deviceTokenService.unregisterAll(user);
        return ResponseEntity.noContent().build();
    }
}
