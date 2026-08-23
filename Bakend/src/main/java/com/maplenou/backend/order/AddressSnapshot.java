package com.maplenou.backend.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Snapshot de l'adresse de livraison pris au moment de la commande.
 * Stocké directement dans la table orders (colonnes préfixées "delivery_").
 * RGPD : pas de FK volatile vers addresses — l'historique reste intact si l'adresse est supprimée.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressSnapshot {

    @Column(name = "delivery_label", length = 100)
    private String label;

    @Column(name = "delivery_city", nullable = false, length = 100)
    private String city;

    @Column(name = "delivery_district", length = 100)
    private String district;

    @Column(name = "delivery_details")
    private String details;

    @Column(name = "delivery_latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "delivery_longitude", precision = 10, scale = 7)
    private BigDecimal longitude;
}
