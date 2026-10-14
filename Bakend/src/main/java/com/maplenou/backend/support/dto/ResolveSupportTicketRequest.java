package com.maplenou.backend.support.dto;

import com.maplenou.backend.support.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;

public record ResolveSupportTicketRequest(
        @NotNull SupportTicketStatus status,
        String resolutionNotes
) {
}
