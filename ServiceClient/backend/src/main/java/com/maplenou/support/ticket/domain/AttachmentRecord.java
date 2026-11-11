package com.maplenou.support.ticket.domain;

import java.time.Instant;
import java.util.UUID;

/** Attachment metadata; the binary lives in the attachment storage under {@code storageKey}. */
public record AttachmentRecord(UUID id, UUID ticketId, UUID messageId, UUID uploadedBy, String originalFilename,
                               String contentType, long sizeBytes, String storageKey, String sha256,
                               Instant createdAt) {
}
