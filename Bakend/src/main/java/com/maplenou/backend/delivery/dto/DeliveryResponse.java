package com.maplenou.backend.delivery.dto;

import com.maplenou.backend.delivery.Delivery;
import com.maplenou.backend.delivery.DeliveryStatus;
import com.maplenou.backend.delivery.ProofType;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID subOrderId,
        UUID shopId,
        String shopName,
        UUID orderId,
        String orderNumber,
        UUID agentId,
        String agentName,
        String agentPhone,
        DeliveryStatus status,
        ProofType proofType,
        String proofUrl,
        String notes,
        Instant assignedAt,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.getId(),
                d.getSubOrder().getId(),
                d.getSubOrder().getShop().getId(),
                d.getSubOrder().getShop().getName(),
                d.getSubOrder().getOrder().getId(),
                d.getSubOrder().getOrder().getOrderNumber(),
                d.getAgent() != null ? d.getAgent().getId() : null,
                d.getAgent() != null ? d.getAgent().getFullName() : null,
                d.getAgent() != null ? d.getAgent().getPhoneNumber() : null,
                d.getStatus(),
                d.getProofType(),
                d.getProofUrl(),
                d.getNotes(),
                d.getAssignedAt(),
                d.getDeliveredAt(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }
}
