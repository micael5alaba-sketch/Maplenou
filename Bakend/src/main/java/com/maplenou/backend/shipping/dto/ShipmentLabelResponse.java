package com.maplenou.backend.shipping.dto;

public record ShipmentLabelResponse(
        String carrierCode,
        String trackingNumber,
        String labelUrl
) {
}
