package com.maplenou.support.notification.domain;

import java.time.Instant;
import java.util.UUID;

/** Firebase Cloud Messaging registration token of a user's device. */
public record DeviceToken(UUID id, UUID userId, String token, Platform platform, Instant createdAt,
                          Instant lastSeenAt) {

    public enum Platform { ANDROID, IOS, WEB }

    public DeviceToken reassign(UUID newUser, Platform newPlatform, Instant now) {
        return new DeviceToken(id, newUser, token, newPlatform, createdAt, now);
    }
}
