package com.maplenou.backend.revenue.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Une ligne du registre des revenus : source, référence de la commande ou livraison, montant, date. */
public record RevenueEntryResponse(
        RevenueSource source,
        UUID referenceId,
        String reference,
        String label,
        BigDecimal amount,
        Instant date
) {}
