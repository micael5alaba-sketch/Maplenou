package com.maplenou.support.ticket.adapter.in.scheduler;

import com.maplenou.support.ticket.application.AutoCloseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the automatic closure of resolved tickets (DEC-CS11-04) every {@code SUPPORT_AUTO_CLOSE_CHECK}. */
@Component
public class AutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseScheduler.class);

    private final AutoCloseService autoClose;

    public AutoCloseScheduler(AutoCloseService autoClose) {
        this.autoClose = autoClose;
    }

    @Scheduled(fixedDelayString = "${maplenou.tickets.auto-close-check:15m}", initialDelayString = "PT1M")
    public void run() {
        try {
            autoClose.closeDue();
        } catch (RuntimeException e) {
            log.warn("Automatic closure failed: {}", e.getMessage());
        }
    }
}
