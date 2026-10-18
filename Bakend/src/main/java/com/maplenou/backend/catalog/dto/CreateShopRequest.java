package com.maplenou.backend.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateShopRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String district,
        // Optionnel : NINEA/TIN/TVA — peut être renseigné plus tard via UpdateShopRequest.
        @Size(max = 50) String taxId,
        // Optionnel : capturées côté app via la position GPS du vendeur (même mécanisme que
        // pour l'adresse acheteur) — indispensables pour le calcul de livraison par distance
        // (voir DeliveryPricingService), sans quoi il retombe sur le tarif de base seul.
        BigDecimal latitude,
        BigDecimal longitude
) {
}
