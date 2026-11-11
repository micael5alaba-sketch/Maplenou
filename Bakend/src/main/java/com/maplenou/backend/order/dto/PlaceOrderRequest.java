package com.maplenou.backend.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlaceOrderRequest(

        @NotNull(message = "L'identifiant de l'adresse de livraison est obligatoire")
        UUID addressId,

        /**
         * Code promo optionnel à appliquer sur la commande.
         * Si fourni, shopId permet de valider un code SHOP.
         */
        String promoCode,

        /** ID de la boutique visée par le code promo (null pour un code PLATFORM). */
        UUID promoShopId,

        /**
         * Pays choisi par l'acheteur pour le paiement Mobile Money (ex: "tg", "bj", "ci") —
         * transmis à FedaPay pour présélectionner le bon pays sur sa page hébergée plutôt que de
         * deviner depuis l'indicatif du numéro de téléphone du compte. Optionnel : si absent,
         * on retombe sur l'ancienne détection par indicatif (voir FedaPayCheckoutService).
         */
        String paymentCountry,

        /**
         * Jetons d'attribution du programme partenaire (un par produit visité via un lien partenaire).
         * Optionnel : sans jeton, seules les attributions rattachées au compte sont utilisées.
         */
        List<String> attributionTokens
) {
    /** Constructeur historique (sans jetons partenaire). */
    public PlaceOrderRequest(UUID addressId, String promoCode, UUID promoShopId, String paymentCountry) {
        this(addressId, promoCode, promoShopId, paymentCountry, null);
    }
}
