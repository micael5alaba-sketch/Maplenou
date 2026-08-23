package com.maplenou.backend.shipping.dto;

import java.time.Instant;

public record CarrierTrackingResponse(
        String trackingNumber,
        String status,
        Instant lastUpdate
) {
}
