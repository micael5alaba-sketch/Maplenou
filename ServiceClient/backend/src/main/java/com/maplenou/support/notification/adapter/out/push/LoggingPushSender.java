package com.maplenou.support.notification.adapter.out.push;

import com.maplenou.support.notification.application.port.PushSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Default sender when Firebase credentials are not configured: logs instead of sending. */
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public List<String> send(PushMessage message) {
        log.info("[push:log-only] to {} device(s): {} — {}", message.tokens().size(), message.title(), message.body());
        return List.of();
    }

    @Override
    public String mode() {
        return "log-only";
    }
}
