package com.maplenou.backend.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FedaPayCustomer(
        String firstname,
        String lastname,
        String email,
        @JsonProperty("phone_number") FedaPayPhoneNumber phoneNumber
) {
}
