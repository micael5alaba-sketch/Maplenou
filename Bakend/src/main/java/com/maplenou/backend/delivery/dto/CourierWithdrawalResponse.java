package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.CourierWithdrawal;
import com.maplenou.backend.payout.PayoutStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourierWithdrawalResponse(
        UUID id,
        UUID agentId,
        String agentName,
        String agentPhone,
        BigDecimal amount,
        PayoutStatus status,
        String paymentDetails,
        UUID approvedBy,
        Instant approvedAt,
        Instant paidAt,
        String notes,
        Instant createdAt
) {
    public static CourierWithdrawalResponse from(CourierWithdrawal w) {
        return new CourierWithdrawalResponse(
                w.getId(),
                w.getAgent().getId(),
                w.getAgent().getFullName(),
                w.getAgent().getPhoneNumber(),
                w.getAmount(),
                w.getStatus(),
                w.getPaymentDetails(),
                w.getApprovedBy(),
                w.getApprovedAt(),
                w.getPaidAt(),
                w.getNotes(),
                w.getCreatedAt()
        );
    }
}
