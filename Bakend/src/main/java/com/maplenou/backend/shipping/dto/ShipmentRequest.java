package com.maplenou.backend.shipping.dto;

public record ShipmentRequest(
        String recipientName,
        String recipientPhone,
        String city,
        String district,
        String details
) {
}
