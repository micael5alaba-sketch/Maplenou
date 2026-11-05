package com.maplenou.backend.courier.dto;

import com.maplenou.backend.courier.CourierLocation;

import java.time.Instant;
import java.util.UUID;

public record CourierLocationResponse(
        UUID agentId,
        String fullName,
        String phoneNumber,
        double latitude,
        double longitude,
        Instant updatedAt
) {
    public static CourierLocationResponse from(CourierLocation l) {
        return new CourierLocationResponse(
                l.getAgentId(),
                l.getAgent().getFullName(),
                l.getAgent().getPhoneNumber(),
                l.getLatitude(),
                l.getLongitude(),
                l.getUpdatedAt()
        );
    }
}
