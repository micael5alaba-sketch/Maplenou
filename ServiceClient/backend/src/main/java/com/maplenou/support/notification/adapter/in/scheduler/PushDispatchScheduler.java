package com.maplenou.support.notification.adapter.in.scheduler;

import com.maplenou.support.notification.application.PushDispatcher;
import com.maplenou.support.platform.config.MaplenouProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PushDispatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(PushDispatchScheduler.class);

    private final PushDispatcher dispatcher;
    private final boolean enabled;

    public PushDispatchScheduler(PushDispatcher dispatcher, MaplenouProperties props) {
        this.dispatcher = dispatcher;
        this.enabled = props.notifications().dispatcherEnabled();
    }

    @Scheduled(fixedDelayString = "${maplenou.notifications.dispatch-interval:5s}")
    public void run() {
        if (!enabled) {
            return;
        }
        try {
            dispatcher.dispatchDue();
        } catch (RuntimeException e) {
            log.warn("Push dispatch cycle failed: {}", e.getMessage());
        }
    }
}
