package com.maplenou.backend.order.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PaymentWebhookRequest(
        @NotBlank(message = "L'identifiant de commande est obligatoire")
        String orderId,

        String paymentReference
) {
    public UUID orderIdAsUUID() {
        return UUID.fromString(orderId);
    }
}
