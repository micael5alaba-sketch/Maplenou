package com.maplenou.backend.returns.dto;

import com.maplenou.backend.returns.ProductCondition;
import com.maplenou.backend.returns.RefundStatus;
import com.maplenou.backend.returns.ReturnDecision;
import com.maplenou.backend.returns.ReturnRequest;

import java.time.Instant;
import java.util.UUID;

public record ReturnRequestResponse(
        UUID id,
        UUID subOrderId,
        UUID shopId,
        String shopName,
        UUID buyerId,
        String buyerName,
        Instant deliveryDate,
        Instant returnDeadline,
        String reason,
        ProductCondition productCondition,
        ReturnDecision decision,
        Instant decidedAt,
        String decidedByName,
        String adminNotes,
        RefundStatus refundStatus,
        Instant createdAt
) {
    public static ReturnRequestResponse from(ReturnRequest r) {
        return new ReturnRequestResponse(
                r.getId(),
                r.getSubOrder().getId(),
                r.getSubOrder().getShop().getId(),
                r.getSubOrder().getShop().getName(),
                r.getBuyer().getId(),
                r.getBuyer().getFullName(),
                r.getDeliveryDate(),
                r.getReturnDeadline(),
                r.getReason(),
                r.getProductCondition(),
                r.getDecision(),
                r.getDecidedAt(),
                r.getDecidedBy() != null ? r.getDecidedBy().getFullName() : null,
                r.getAdminNotes(),
                r.getRefundStatus(),
                r.getCreatedAt()
        );
    }
}
