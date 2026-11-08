package com.maplenou.support.ticket.adapter.out.persistence;

import com.maplenou.support.ticket.application.port.ConversationStore;
import com.maplenou.support.ticket.domain.AttachmentRecord;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.TicketMessage;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaConversationStore implements ConversationStore {

    private final TicketMessageJpaRepository messages;
    private final TicketAttachmentJpaRepository attachments;

    JpaConversationStore(TicketMessageJpaRepository messages, TicketAttachmentJpaRepository attachments) {
        this.messages = messages;
        this.attachments = attachments;
    }

    @Override
    public void save(TicketMessage message) {
        messages.save(new TicketMessageEntity(message));
    }

    @Override
    public Optional<TicketMessage> findMessage(UUID id) {
        return messages.findById(id).map(TicketMessageEntity::toDomain);
    }

    @Override
    public Optional<TicketMessage> findByClientMessageId(UUID ticketId, UUID authorId, String clientMessageId) {
        return messages.findByTicketIdAndAuthorIdAndClientMessageId(ticketId, authorId, clientMessageId)
                .map(TicketMessageEntity::toDomain);
    }

    @Override
    public List<TicketMessage> messages(UUID ticketId, Collection<MessageVisibility> visibilities) {
        return messages.findByTicketIdAndVisibilityInOrderByCreatedAtAsc(ticketId, visibilities).stream()
                .map(TicketMessageEntity::toDomain).toList();
    }

    @Override
    public void save(AttachmentRecord attachment) {
        attachments.save(new TicketAttachmentEntity(attachment));
    }

    @Override
    public Optional<AttachmentRecord> findAttachment(UUID id) {
        return attachments.findById(id).map(TicketAttachmentEntity::toDomain);
    }

    @Override
    public long totalAttachmentBytes(UUID ticketId) {
        return attachments.totalBytesForTicket(ticketId);
    }

    @Override
    public List<AttachmentRecord> attachmentsOf(Collection<UUID> messageIds) {
        return attachments.findByMessageIdInOrderByCreatedAtAsc(messageIds).stream()
                .map(TicketAttachmentEntity::toDomain).toList();
    }
}
