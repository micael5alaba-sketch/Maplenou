package com.maplenou.support.ticket.adapter.in.web;

import com.maplenou.support.ticket.application.TicketCommands;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request bodies of the ticket API (form validation; the use cases check the meaning). */
public final class TicketRequests {

    private TicketRequests() {
    }

    /**
     * Ticket creation. {@code priority} is the requester's assessment (CRITICAL is rejected, BR-036);
     * {@code urgent} records reported urgency without changing priority (BR-037).
     */
    public record CreateTicket(
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 5000) String description,
            @NotBlank @Size(max = 30) String categoryCode,
            @Size(max = 20) String priority,
            Boolean urgent,
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-]*$") String orderRef,
            @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9-]*$") String deliveryRef) {
    }

    /** Customer-visible message or internal note; {@code clientMessageId} makes retries idempotent. */
    public record PostMessage(
            @NotBlank @Size(max = 5000) String body,
            @Size(max = 64) @Pattern(regexp = "^[A-Za-z0-9_-]*$") String clientMessageId) {
    }

    public record ChangeStatus(@NotBlank @Size(max = 25) String status, @Size(max = 500) String comment) {
    }

    public record ChangePriority(@NotBlank @Size(max = 20) String priority, @Size(max = 500) String reason) {
    }

    public record ChangeCategory(@NotBlank @Size(max = 30) String categoryCode) {
    }

    /** {@code agentId} null means "assign to me". */
    public record Assign(UUID agentId) {
    }

    public record Escalate(@NotBlank @Size(max = 500) String reason) {
    }

    public record RequestReopen(@NotBlank @Size(max = 1000) String reason) {
    }

    /** {@code targetStatus} defaults to "in progress" when approving (BR-051). */
    public record DecideReopen(@NotNull TicketCommands.Decision decision, @Size(max = 1000) String note,
                               @Size(max = 25) String targetStatus) {
    }

    public record Rate(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {
    }

    public record RequestRefund(@Size(max = 60) String subOrderId, @NotBlank @Size(min = 10, max = 900) String reason,
                                @Pattern(regexp = "^(NEW|GOOD|DAMAGED)?$") String productCondition) {
    }

    public record RescheduleDelivery(@NotBlank @Size(max = 500) String reason) {
    }

    public record ContactDriver(@NotBlank @Size(max = 1000) String message) {
    }

    public record AiDecision(@NotNull Boolean apply) {
    }

    public record DraftRequest(@Pattern(regexp = "^(fr|en)?$") String language) {
    }
}
