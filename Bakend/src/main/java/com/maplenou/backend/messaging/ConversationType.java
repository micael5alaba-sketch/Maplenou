package com.maplenou.backend.messaging;

public enum ConversationType {
    /** Échange entre un client et une boutique. */
    BUYER_SELLER,
    /** Échange entre un utilisateur (client ou vendeur) et l'équipe support/admin. */
    SUPPORT,
    /** Échange entre un client et le livreur assigné à sa livraison en cours. */
    BUYER_COURIER,
    /** Échange entre une boutique et le livreur assigné à une de ses livraisons. */
    SELLER_COURIER
}
