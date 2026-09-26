package com.maplenou.backend.courier.dto;

import com.maplenou.backend.courier.CourierDocumentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ReviewCourierDocumentRequest(
        @NotNull CourierDocumentStatus decision,
        String rejectionReason
) {
    /** L'admin ne peut trancher qu'en VALID ou REJECTED — PENDING/EXPIRED ne sont pas des décisions. */
    @AssertTrue(message = "La décision doit être VALID ou REJECTED")
    public boolean isDecisionValid() {
        return decision == CourierDocumentStatus.VALID || decision == CourierDocumentStatus.REJECTED;
    }
}
