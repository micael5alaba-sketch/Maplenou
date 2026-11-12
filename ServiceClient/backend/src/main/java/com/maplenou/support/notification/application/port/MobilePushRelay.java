package com.maplenou.support.notification.application.port;

import java.util.Map;
import java.util.UUID;

/**
 * Push to the Maplenou mobile apps through Bakend, which knows the devices the apps registered (DEC-CS11-09).
 * Used for requesters: they install the Maplenou app, not a separate support app.
 */
public interface MobilePushRelay {

    boolean enabled();

    /** @throws RuntimeException when Bakend could not be reached (the dispatcher retries) */
    void send(UUID userId, String title, String body, Map<String, String> data);
}
