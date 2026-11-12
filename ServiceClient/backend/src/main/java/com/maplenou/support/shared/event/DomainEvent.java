package com.maplenou.support.shared.event;

/**
 * Something that happened in a module and that other modules may react to. Events are stored in the
 * transactional outbox in the same transaction as the change, then delivered (at least once) to the listeners.
 * Implementations must be immutable records made of plain values (they are serialized to JSON).
 */
public interface DomainEvent {
}
