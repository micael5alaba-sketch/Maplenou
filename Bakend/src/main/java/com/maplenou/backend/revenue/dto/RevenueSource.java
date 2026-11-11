package com.maplenou.backend.revenue.dto;

public enum RevenueSource {
    /** Commission intégrée au prix (5 % / 3 %), reconnue à la livraison de la sous-commande. */
    COMMISSION,
    /** Part de la plateforme (25 %) sur les frais de livraison d'une livraison terminée. */
    DELIVERY_SHARE,
    /** Frais mensuel vendeur effectivement déduit d'un reversement. */
    SELLER_FEE
}
