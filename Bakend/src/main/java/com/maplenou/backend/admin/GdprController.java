package com.maplenou.backend.admin;

import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/gdpr")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "RGPD Admin")
@SecurityRequirement(name = "bearerAuth")
public class GdprController {

    private final GdprService gdprService;

    @PostMapping("/users/{userId}/anonymize")
    public ResponseEntity<Void> anonymizeUser(@PathVariable UUID userId,
                                               @AuthenticationPrincipal User currentUser,
                                               HttpServletRequest request) {
        gdprService.anonymizeUser(currentUser, userId, extractIp(request));
        return ResponseEntity.noContent().build();
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
