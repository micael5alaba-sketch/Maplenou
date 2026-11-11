package com.maplenou.backend.user;

/**
 * Roles internes uniquement. Tout compte est acheteur par defaut (pas besoin de role explicite).
 * Le profil vendeur n'est PAS un role : voir SellerProfile, optionnel et rattache a un User.
 *
 * Personnel du back-office (document "Roles et acces au tableau de bord") : {@link #ADMIN} est le
 * Super Admin (nom conserve pour les comptes existants), les trois autres sont des roles a acces
 * limite. Les droits de chaque role sont centralises dans
 * {@link com.maplenou.backend.security.RolePermissions}.
 */
public enum Role {
    /** Super Admin : acces total, comptes du personnel, approbation finale des retraits. */
    ADMIN,
    DELIVERY_AGENT,
    /** Administrateur des livraisons : livraisons, livreurs, documents, gains (lecture). */
    DELIVERY_ADMIN,
    /** Agent support et retraits : support, vendeurs, retours, preparation des retraits. */
    SUPPORT_AGENT,
    /** Auditeur : consultation uniquement. */
    AUDITOR;

    /** Compte du personnel du back-office (jamais cree par inscription publique). */
    public boolean isStaff() {
        return this == ADMIN || this == DELIVERY_ADMIN || this == SUPPORT_AGENT || this == AUDITOR;
    }
}
