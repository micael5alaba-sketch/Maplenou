package com.maplenou.support.platform.outbox;

import com.maplenou.support.shared.event.DomainEvent;
import com.maplenou.support.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * Transactional outbox (reference architecture §1.3): the event is written in the same transaction as the change,
 * then delivered by {@link OutboxRelay} right after the commit — or by its poller if the process stopped first.
 * Notifications therefore survive a crash between the commit and their creation.
 */
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outbox;
    private final OutboxRelay relay;
    private final JsonMapper json;
    private final Clock clock;

    public OutboxEventPublisher(OutboxRepository outbox, OutboxRelay relay, JsonMapper json, Clock clock) {
        this.outbox = outbox;
        this.relay = relay;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void publish(DomainEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Domain events are published inside the transaction of the change");
        }
        UUID id = UUID.randomUUID();
        outbox.save(new OutboxEventEntity(id, event.getClass().getName(), json.writeValueAsString(event), clock.instant()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                relay.deliverSoon(id);
            }
        });
    }
}
