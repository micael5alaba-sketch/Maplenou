package com.maplenou.backend.payout.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Requête admin pour déclencher manuellement la génération de payouts sur une période donnée.
 * Utile pour les tests et les corrections manuelles.
 */
public record GeneratePayoutsRequest(
        @NotNull Instant periodStart,
        @NotNull Instant periodEnd
) {}
