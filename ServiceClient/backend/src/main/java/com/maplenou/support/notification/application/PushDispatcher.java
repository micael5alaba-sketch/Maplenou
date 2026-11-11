package com.maplenou.support.notification.application;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.notification.application.port.DeviceTokenStore;
import com.maplenou.support.notification.application.port.MobilePushRelay;
import com.maplenou.support.notification.application.port.NotificationStore;
import com.maplenou.support.notification.application.port.PushSender;
import com.maplenou.support.notification.domain.DeviceToken;
import com.maplenou.support.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Delivers pending push notifications with retries and exponential backoff. Works only on notifications, never on
 * tickets (BR-020). Two channels: the devices registered with this service (Firebase), and — for requesters, when
 * enabled — the Maplenou mobile apps through Bakend (DEC-CS11-09).
 */
@Service
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);
    private static final int BATCH = 50;

    private final NotificationStore notifications;
    private final DeviceTokenStore deviceTokens;
    private final PushSender sender;
    private final MobilePushRelay relay;
    private final UserDirectory directory;
    private final NotificationSettings settings;
    private final Clock clock;

    public PushDispatcher(NotificationStore notifications, DeviceTokenStore deviceTokens, PushSender sender,
                          MobilePushRelay relay, UserDirectory directory, NotificationSettings settings, Clock clock) {
        this.notifications = notifications;
        this.deviceTokens = deviceTokens;
        this.sender = sender;
        this.relay = relay;
        this.directory = directory;
        this.settings = settings;
        this.clock = clock;
    }

    /** Processes one batch; returns the number of notifications handled. */
    @Transactional
    public int dispatchDue() {
        Instant now = clock.instant();
        List<UUID> ids = notifications.lockDuePush(now, BATCH);
        for (UUID id : ids) {
            notifications.find(id).ifPresent(n -> {
                deliver(n, now);
                notifications.save(n);
            });
        }
        return ids.size();
    }

    private void deliver(Notification n, Instant now) {
        List<DeviceToken> tokens = deviceTokens.forUser(n.recipientId());
        boolean viaApp = relay.enabled() && directory.find(n.recipientId())
                .map(u -> u.role() != null && u.role().isExternalRequester()).orElse(false);
        if (tokens.isEmpty() && !viaApp) {
            n.pushSkipped();
            return;
        }
        Map<String, String> data = new java.util.HashMap<>();
        data.put("type", n.type().name());
        data.put("source", "support");
        if (n.ticketId() != null) {
            data.put("ticketId", n.ticketId().toString());
            String app = settings.appUrl() == null ? "" : settings.appUrl().replaceAll("/+$", "");
            if (!app.isBlank()) {
                data.put("url", app + "/?ticket=" + n.ticketId());
            }
        }
        try {
            if (!tokens.isEmpty()) {
                List<String> invalid = sender.send(new PushSender.PushMessage(
                        tokens.stream().map(DeviceToken::token).toList(), n.title(), n.body(), data));
                invalid.forEach(deviceTokens::deleteByToken);
            }
            if (viaApp) {
                relay.send(n.recipientId(), n.title(), n.body(), data);
            }
            n.pushSent(now);
        } catch (PushSender.PushDeliveryException | RuntimeException e) {
            log.warn("Push delivery failed for notification {} (attempt {}): {}", n.id(), n.pushAttempts() + 1,
                    e.getMessage());
            n.pushFailed(e.getMessage(), now, settings.maxAttempts());
        }
    }
}
