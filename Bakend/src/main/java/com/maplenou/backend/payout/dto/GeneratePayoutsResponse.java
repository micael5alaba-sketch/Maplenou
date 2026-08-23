package com.maplenou.backend.payout.dto;

import java.time.Instant;

public record GeneratePayoutsResponse(
        int payoutsCreated,
        Instant periodStart,
        Instant periodEnd
) {}
