package com.maplenou.backend.newsletter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
        @NotBlank @Email String email
) {
}
