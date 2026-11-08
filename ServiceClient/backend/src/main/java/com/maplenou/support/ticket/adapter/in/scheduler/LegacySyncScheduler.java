package com.maplenou.support.ticket.adapter.in.scheduler;

import com.maplenou.support.shared.SupportException;
import com.maplenou.support.ticket.application.LegacyImportService;
import com.maplenou.support.ticket.application.LegacyImportService.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps ServiceClient in step with the former support module while the Flutter app still opens tickets there
 * (DEC-CS12-02): {@code LEGACY_SYNC_ENABLED=true} runs the idempotent import every {@code LEGACY_SYNC_INTERVAL}.
 */
@Component
@ConditionalOnProperty(name = "maplenou.legacy.sync-enabled", havingValue = "true")
public class LegacySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(LegacySyncScheduler.class);

    private final LegacyImportService importer;

    public LegacySyncScheduler(LegacyImportService importer) {
        this.importer = importer;
    }

    @Scheduled(fixedDelayString = "${maplenou.legacy.sync-interval:PT15M}", initialDelayString = "PT2M")
    public void run() {
        try {
            ImportResult r = importer.synchronize();
            if (r.imported() > 0 || r.updated() > 0) {
                log.info("Legacy support sync: {} imported, {} status change(s) noted, {} in the old module",
                        r.imported(), r.updated(), r.totalInSource());
            }
        } catch (SupportException e) {
            log.warn("Legacy support sync skipped: {} ({})", e.getMessage(), e.code());
        } catch (RuntimeException e) {
            log.warn("Legacy support sync failed: {}", e.getMessage());
        }
    }
}
