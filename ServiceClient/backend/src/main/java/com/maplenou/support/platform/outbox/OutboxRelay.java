package com.maplenou.support.platform.outbox;

import com.maplenou.support.platform.config.MaplenouProperties;
import com.maplenou.support.shared.event.DomainEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Delivers outbox events to the listeners of the other modules ({@code @EventListener} methods), each event in its
 * own transaction together with the "published" mark: the listeners' writes and the mark are committed together,
 * so an event is handled once even when delivery is retried. Failures are retried with an exponential backoff;
 * after {@code maplenou.outbox.max-attempts} the event is kept as failed for investigation (metric
 * {@code support.outbox.failed}).
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 50;

    private final OutboxRepository outbox;
    private final ApplicationEventPublisher listeners;
    private final JsonMapper json;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final int maxAttempts;

    public OutboxRelay(OutboxRepository outbox, ApplicationEventPublisher listeners, JsonMapper json,
                       PlatformTransactionManager txManager, Clock clock, MaplenouProperties props,
                       MeterRegistry meters) {
        this.outbox = outbox;
        this.listeners = listeners;
        this.json = json;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
        this.maxAttempts = props.outbox().maxAttempts();
        Gauge.builder("support.outbox.pending", outbox, OutboxRepository::countPending)
                .description("Events not delivered yet").register(meters);
        Gauge.builder("support.outbox.failed", outbox, OutboxRepository::countFailed)
                .description("Events given up after the maximum number of attempts").register(meters);
    }

    /** Right after the commit that wrote the event: deliver it now (the poller catches up on failure). */
    public void deliverSoon(UUID id) {
        try {
            deliver(id);
        } catch (RuntimeException e) {
            log.warn("Outbox event {} not delivered right after commit, will retry: {}", id, e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${maplenou.outbox.poll-interval:5s}")
    public void poll() {
        try {
            deliverDue();
        } catch (RuntimeException e) {
            log.warn("Outbox poll failed: {}", e.getMessage());
        }
    }

    /** Delivers the events that are due; returns how many were handled. */
    public int deliverDue() {
        int handled = 0;
        for (UUID id : outbox.findDue(clock.instant(), BATCH)) {
            deliver(id);
            handled++;
        }
        return handled;
    }

    private void deliver(UUID id) {
        try {
            tx.executeWithoutResult(status -> outbox.lockUndelivered(id).ifPresent(row -> {
                listeners.publishEvent(read(row));
                row.published(clock.instant());
            }));
        } catch (RuntimeException e) {
            log.warn("Outbox event {} failed: {}", id, e.getMessage());
            tx.executeWithoutResult(status -> outbox.lockUndelivered(id)
                    .ifPresent(row -> row.failedAttempt(e.getMessage(), clock.instant(), maxAttempts)));
        }
    }

    private DomainEvent read(OutboxEventEntity row) {
        Class<?> type;
        try {
            type = Class.forName(row.getEventType());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unknown event type " + row.getEventType(), e);
        }
        if (!DomainEvent.class.isAssignableFrom(type)) {
            throw new IllegalStateException("Not a domain event: " + row.getEventType());
        }
        return (DomainEvent) json.readValue(row.getPayload(), type);
    }
}
