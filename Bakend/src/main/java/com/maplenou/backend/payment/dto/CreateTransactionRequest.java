package com.maplenou.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Corps de `POST /v1/transactions` — voir `docs.fedapay.com/api-reference/transactions`. */
public record CreateTransactionRequest(
        String description,
        int amount,
        FedaPayCurrency currency,
        FedaPayCustomer customer,
        @JsonProperty("callback_url") String callbackUrl
) {
}
