package com.maplenou.support.shared.event;

/**
 * Port used by application services to publish domain events. The outbox adapter writes the event in the
 * current transaction: if the transaction rolls back, the event is never delivered; if the process stops after
 * the commit, the event is delivered when it starts again.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
