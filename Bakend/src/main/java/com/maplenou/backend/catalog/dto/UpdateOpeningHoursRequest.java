package com.maplenou.backend.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Remplace l'horaire complet de la boutique en un seul appel — plus simple qu'un CRUD par
 * jour pour un maximum de 7 entrées. */
public record UpdateOpeningHoursRequest(
        @NotNull @Size(max = 7) List<@Valid OpeningHourEntry> hours
) {
}
