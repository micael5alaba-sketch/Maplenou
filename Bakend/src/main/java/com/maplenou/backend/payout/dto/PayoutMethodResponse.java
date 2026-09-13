package com.maplenou.backend.payout.dto;

import com.maplenou.backend.payout.PayoutMethod;
import com.maplenou.backend.payout.PayoutMethodType;

import java.time.Instant;
import java.util.UUID;

public record PayoutMethodResponse(
        UUID id,
        PayoutMethodType type,
        String accountNumber,
        boolean isDefault,
        Instant createdAt
) {
    public static PayoutMethodResponse from(PayoutMethod m) {
        return new PayoutMethodResponse(
                m.getId(),
                m.getType(),
                m.getAccountNumber(),
                m.isDefault(),
                m.getCreatedAt()
        );
    }
}
