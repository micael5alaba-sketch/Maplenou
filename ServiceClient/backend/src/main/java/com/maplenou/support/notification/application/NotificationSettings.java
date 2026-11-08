package com.maplenou.support.notification.application;

import java.time.Duration;

/**
 * Push delivery settings, built from the configuration by the adapter layer.
 *
 * @param appUrl public address of the web app ({@code SUPPORT_APP_URL}); pushes carry {@code url} =
 *               {@code <appUrl>/?ticket=<id>} so the Maplenou mobile app can open the ticket. Empty = no link.
 */
public record NotificationSettings(int maxAttempts, Duration templateCacheTtl, String appUrl) {

    public NotificationSettings(int maxAttempts, Duration templateCacheTtl) {
        this(maxAttempts, templateCacheTtl, "");
    }
}
