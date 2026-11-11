package com.maplenou.backend.withdrawal.dto;

import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.withdrawal.WithdrawalStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record WithdrawalHistoryResponse(
        UUID id,
        PayoutStatus fromStatus,
        PayoutStatus toStatus,
        UUID actorId,
        String note,
        Instant createdAt
) {
    public static WithdrawalHistoryResponse from(WithdrawalStatusHistory h) {
        return new WithdrawalHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getActorId(), h.getNote(), h.getCreatedAt());
    }
}
