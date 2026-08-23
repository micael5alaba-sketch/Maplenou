package com.maplenou.backend.delivery.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Données de navigation transmises à l'app Flutter du livreur.
 * Flutter utilise ces coordonnées pour ouvrir Google Maps avec :
 *   1. L'itinéraire vers le point de retrait (boutique vendeur)
 *   2. L'itinéraire vers le point de livraison (adresse client)
 */
public record NavigationResponse(
        UUID deliveryId,
        PickupPoint pickup,
        DropoffPoint dropoff
) {
    public record PickupPoint(
            UUID shopId,
            String shopName,
            String address,     // "Lomé, Bè" — affiché dans l'app
            BigDecimal latitude,
            BigDecimal longitude
    ) {}

    public record DropoffPoint(
            String clientName,
            String address,     // "Lomé, Adidogomé" — affiché dans l'app
            BigDecimal latitude,
            BigDecimal longitude
    ) {}
}
