package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.platform.persistence.BaseEntity;
import com.maplenou.support.ticket.domain.AttachmentRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ticket_attachments")
class TicketAttachmentEntity extends BaseEntity {

    @Column(name = "ticket_id", nullable = false, updatable = false)
    private UUID ticketId;
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;
    @Column(name = "uploaded_by", nullable = false, updatable = false)
    private UUID uploadedBy;
    @Column(name = "original_filename", nullable = false, updatable = false)
    private String originalFilename;
    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;
    @Column(name = "storage_key", nullable = false, unique = true, updatable = false)
    private String storageKey;
    @Column(nullable = false, updatable = false)
    private String sha256;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketAttachmentEntity() {
    }

    TicketAttachmentEntity(AttachmentRecord a) {
        super(a.id());
        this.ticketId = a.ticketId();
        this.messageId = a.messageId();
        this.uploadedBy = a.uploadedBy();
        this.originalFilename = a.originalFilename();
        this.contentType = a.contentType();
        this.sizeBytes = a.sizeBytes();
        this.storageKey = a.storageKey();
        this.sha256 = a.sha256();
        this.createdAt = a.createdAt();
    }

    AttachmentRecord toDomain() {
        return new AttachmentRecord(getId(), ticketId, messageId, uploadedBy, originalFilename, contentType, sizeBytes,
                storageKey, sha256, createdAt);
    }
}
