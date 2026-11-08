package com.maplenou.support.ticket.application.port;

import com.maplenou.support.ticket.domain.AttachmentRecord;
import com.maplenou.support.ticket.domain.MessageVisibility;
import com.maplenou.support.ticket.domain.TicketMessage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Messages, internal notes and attachment metadata of tickets. */
public interface ConversationStore {

    void save(TicketMessage message);

    Optional<TicketMessage> findMessage(UUID id);

    /** A retry of an already accepted message (AC-SB-006). */
    Optional<TicketMessage> findByClientMessageId(UUID ticketId, UUID authorId, String clientMessageId);

    List<TicketMessage> messages(UUID ticketId, Collection<MessageVisibility> visibilities);

    void save(AttachmentRecord attachment);

    Optional<AttachmentRecord> findAttachment(UUID id);

    long totalAttachmentBytes(UUID ticketId);

    List<AttachmentRecord> attachmentsOf(Collection<UUID> messageIds);
}
