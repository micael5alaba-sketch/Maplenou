package com.maplenou.support.ticket.application;

import com.maplenou.support.attachment.domain.AttachmentPolicy.IncomingFile;

import java.util.List;
import java.util.UUID;

/** Inputs of the ticket use cases (validated for form by the web adapter, for meaning by the use cases). */
public final class TicketCommands {

    private TicketCommands() {
    }

    /**
     * Ticket creation. {@code priority} is the requester's assessment (only requester-selectable priorities,
     * BR-036); {@code urgent} records reported urgency without changing priority (BR-037).
     */
    public record CreateTicket(String subject, String description, String categoryCode, String priority,
                               Boolean urgent, String orderRef, String deliveryRef, List<IncomingFile> files) {
    }

    public record PostMessage(String body, String clientMessageId, List<IncomingFile> files) {
    }

    public record ChangeStatus(String status, String comment) {
    }

    public record ChangePriority(String priority, String reason) {
    }

    /** {@code agentId} null means "assign to me". */
    public record Assign(UUID agentId) {
    }

    public enum Decision { APPROVE, REJECT }

    /** {@code targetStatus} defaults to the system "in progress" status when approving (BR-051). */
    public record DecideReopen(Decision decision, String note, String targetStatus) {
    }

    public record Rate(int rating, String comment) {
    }

    public record RequestRefund(String subOrderId, String reason, String productCondition) {
    }
}
