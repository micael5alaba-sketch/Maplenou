package com.maplenou.backend.delivery.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignAgentRequest(
        @NotNull(message = "L'identifiant du livreur est obligatoire")
        UUID agentId
) {}
