package com.maplenou.backend.catalog.dto;

import com.maplenou.backend.catalog.ShopOpeningHour;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Un jour de la semaine dans l'horaire d'ouverture d'une boutique — utilisé à la fois en
 * sortie (fiche boutique publique) et en entrée (mise à jour par le vendeur, voir
 * {@link UpdateOpeningHoursRequest}).
 */
public record OpeningHourEntry(
        /** 1 = lundi ... 7 = dimanche. */
        @Min(1) @Max(7) int dayOfWeek,
        /** Format "HH:mm", null si fermé ce jour-là. */
        String opensAt,
        String closesAt,
        boolean closed
) {
    public static OpeningHourEntry from(ShopOpeningHour hour) {
        return new OpeningHourEntry(
                hour.getDayOfWeek(),
                hour.getOpensAt() != null ? hour.getOpensAt().toString() : null,
                hour.getClosesAt() != null ? hour.getClosesAt().toString() : null,
                hour.isClosed()
        );
    }
}
