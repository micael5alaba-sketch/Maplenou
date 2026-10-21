package com.maplenou.backend.notification;

import com.maplenou.backend.notification.dto.NotificationResponse;
import com.maplenou.backend.user.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Centre de notifications persistant — absent jusqu'ici (signalé par l'audit externe du
 * 16/10/2026), voir {@link Notification}. Les tokens FCM restent sur {@link DeviceTokenController}.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Page<NotificationResponse> listMine(@AuthenticationPrincipal User currentUser,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.listMine(currentUser, pageable);
    }

    /** Compteur pour le badge de l'app. */
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal User currentUser) {
        return Map.of("count", notificationService.countUnread(currentUser));
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        notificationService.markAsRead(currentUser, id);
    }

    /** Marquer toutes mes notifications comme lues (ex: à l'ouverture de l'écran notifications). */
    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser);
    }
}
