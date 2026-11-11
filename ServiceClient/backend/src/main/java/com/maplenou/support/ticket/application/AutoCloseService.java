package com.maplenou.support.ticket.application;

import com.maplenou.support.ticket.application.port.TicketSearch;
import com.maplenou.support.ticket.domain.vocabulary.StatusKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Closes resolved tickets when the requester did not ask to reopen them within {@code SUPPORT_AUTO_CLOSE_AFTER}
 * (7 days by default, DEC-CS11-04). Each ticket is closed in its own transaction with a row lock, so several
 * instances may run it at the same time.
 */
@Service
public class AutoCloseService {

    private static final Logger log = LoggerFactory.getLogger(AutoCloseService.class);
    private static final int BATCH = 200;

    private final TicketSearch search;
    private final TicketCommandService commands;
    private final VocabularyService vocabulary;
    private final Duration after;
    private final Clock clock;

    public AutoCloseService(TicketSearch search, TicketCommandService commands, VocabularyService vocabulary,
                            TicketSettings settings, Clock clock) {
        this.search = search;
        this.commands = commands;
        this.vocabulary = vocabulary;
        this.after = settings.autoCloseAfter();
        this.clock = clock;
    }

    public boolean enabled() {
        return after != null && !after.isZero() && !after.isNegative();
    }

    /** Returns the number of tickets closed. */
    public int closeDue() {
        if (!enabled()) {
            return 0;
        }
        Instant before = clock.instant().minus(after);
        List<UUID> due = search.resolvedBefore(vocabulary.workflow().codesOfKind(StatusKind.RESOLVED), before, BATCH);
        int closed = 0;
        for (UUID id : due) {
            try {
                if (commands.autoClose(id, before)) {
                    closed++;
                }
            } catch (RuntimeException e) {
                log.warn("Automatic closure of ticket {} failed: {}", id, e.getMessage());
            }
        }
        if (closed > 0) {
            log.info("{} resolved ticket(s) closed automatically", closed);
        }
        return closed;
    }
}
