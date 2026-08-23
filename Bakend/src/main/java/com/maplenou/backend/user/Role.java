package com.maplenou.backend.user;

/**
 * Roles internes uniquement. Tout compte est acheteur par defaut (pas besoin de role explicite).
 * Le profil vendeur n'est PAS un role : voir SellerProfile, optionnel et rattache a un User.
 */
public enum Role {
    ADMIN,
    DELIVERY_AGENT
}
