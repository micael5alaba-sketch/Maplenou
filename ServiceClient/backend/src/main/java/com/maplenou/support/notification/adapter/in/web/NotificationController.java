package com.maplenou.support.notification.adapter.in.web;

import com.maplenou.support.notification.application.NotificationService;
import com.maplenou.support.notification.application.PushDiagnostics;
import com.maplenou.support.notification.application.TemplateService;
import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.notification.domain.MessageTemplate;
import com.maplenou.support.notification.domain.NotificationType;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.shared.PageQuery;
import com.maplenou.support.shared.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Notifications")
public class NotificationController {

    private static final String BASE = "/api/v1/support/notifications";

    private final NotificationService notifications;
    private final PushDiagnostics push;
    private final TemplateService templates;
    private final CurrentActorProvider currentUser;

    public NotificationController(NotificationService notifications, PushDiagnostics push, TemplateService templates,
                                  CurrentActorProvider currentUser) {
        this.notifications = notifications;
        this.push = push;
        this.templates = templates;
        this.currentUser = currentUser;
    }

    @GetMapping(BASE)
    public PageResult<NotificationService.NotificationView> list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notifications.list(currentUser.get(), unreadOnly, new PageQuery(page, size));
    }

    @GetMapping(BASE + "/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notifications.unreadCount(currentUser.get()));
    }

    @PostMapping(BASE + "/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id) {
        notifications.markRead(currentUser.get(), id);
    }

    @PostMapping(BASE + "/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notifications.markAllRead(currentUser.get()));
    }

    public record DeviceRegistration(@NotBlank @Size(max = 512) String token, @NotNull DeviceToken.Platform platform) {
    }

    /** Registers a Firebase Cloud Messaging token for the caller's device (Flutter/web apps). */
    @PostMapping(BASE + "/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerDevice(@Valid @RequestBody DeviceRegistration body) {
        notifications.registerDevice(currentUser.get(), body.token(), body.platform());
    }

    /** Push channel of the server ("firebase" or "log-only") and the number of the caller's registered devices. */
    @GetMapping(BASE + "/push-status")
    public PushDiagnostics.PushStatus pushStatus() {
        return push.status(currentUser.get());
    }

    /** Sends a test push to the caller's registered devices at once (422 NO_DEVICE_REGISTERED when none). */
    @PostMapping(BASE + "/devices/test")
    public PushDiagnostics.PushTestResult testPush() {
        return push.sendTest(currentUser.get());
    }

    @DeleteMapping(BASE + "/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDevice(@PathVariable String token) {
        notifications.unregisterDevice(currentUser.get(), token);
    }

    // ---------------------------------------------------------------- templates (administrators)

    @GetMapping("/api/v1/support/admin/notification-templates")
    @PreAuthorize("hasAnyRole('SUPPORT_MANAGER','ADMINISTRATOR')")
    public List<MessageTemplate> templates() {
        return templates.list();
    }

    public record TemplateUpdate(@NotBlank @Size(max = 160) String title, @NotBlank @Size(max = 500) String body) {
    }

    @PutMapping("/api/v1/support/admin/notification-templates/{type}/{language}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public MessageTemplate updateTemplate(@PathVariable NotificationType type, @PathVariable String language,
                                          @Valid @RequestBody TemplateUpdate body) {
        return templates.update(currentUser.get(), type, language, body.title(), body.body());
    }
}
