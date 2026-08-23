package com.maplenou.backend.order.dto;

import jakarta.validation.constraints.NotNull;

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
        UUID promoShopId
) {}
