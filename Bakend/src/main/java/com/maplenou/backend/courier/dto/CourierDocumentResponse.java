package com.maplenou.backend.courier.dto;

import com.maplenou.backend.courier.CourierDocument;
import com.maplenou.backend.courier.CourierDocumentStatus;
import com.maplenou.backend.courier.CourierDocumentType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CourierDocumentResponse(
        UUID id,
        UUID courierId,
        String courierName,
        CourierDocumentType type,
        String fileUrl,
        CourierDocumentStatus status,
        LocalDate expiresAt,
        String rejectionReason,
        Instant submittedAt,
        Instant reviewedAt
) {
    public static CourierDocumentResponse from(CourierDocument d) {
        return new CourierDocumentResponse(
                d.getId(),
                d.getCourier().getId(),
                d.getCourier().getFullName(),
                d.getType(),
                d.getFileUrl(),
                d.effectiveStatus(),
                d.getExpiresAt(),
                d.getRejectionReason(),
                d.getSubmittedAt(),
                d.getReviewedAt()
        );
    }
}
