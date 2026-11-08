package com.maplenou.support.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Pre-written answer support inserts in the composer (« Réponse type », mockup 13). Placeholders
 * {@code {client}}, {@code {ticket}} and {@code {agent}} are replaced when inserted; the agent can still edit.
 */
public record CannedReply(UUID id, String title, String body, String language, String categoryCode, boolean active,
                          UUID updatedBy, Instant updatedAt) {
}
