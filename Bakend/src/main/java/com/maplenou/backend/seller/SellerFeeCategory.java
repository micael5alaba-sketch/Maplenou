package com.maplenou.backend.seller;

import java.math.BigDecimal;

/**
 * Catégorie tarifaire choisie par le vendeur à l'inscription (ou changée plus tard, effective au
 * mois suivant seulement — voir {@code com.maplenou.backend.sellerfee.SellerFeeService}).
 * Déterminisme du frais mensuel isolé ici, comme {@code CommissionService} pour la commission —
 * facilement ajustable sans toucher au reste du système de facturation.
 */
public enum SellerFeeCategory {

    /** Produits de 500 à 10 000 FCFA. */
    STANDARD,

    /** Au moins un produit déclaré à plus de 10 000 FCFA. */
    HIGH_PRICE;

    public BigDecimal monthlyFeeAmount() {
        return switch (this) {
            case STANDARD -> new BigDecimal("500.00");
            case HIGH_PRICE -> new BigDecimal("2000.00");
        };
    }
}
