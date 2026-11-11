package com.maplenou.backend.security;

/**
 * Permissions du back-office, verifiees COTE SERVEUR sur chaque route
 * ({@code @PreAuthorize("hasAuthority('NOM')")}), jamais seulement par masquage des menus.
 * Le nom de l'enum EST le nom de l'autorite Spring Security. Lecture et ecriture sont separees
 * pour que l'Auditeur n'ait jamais que la lecture.
 *
 * L'attribution aux roles vit dans un seul endroit : {@link RolePermissions}.
 */
public enum Permission {
    DASHBOARD_VIEW,

    SELLER_APPLICATIONS_READ, SELLER_APPLICATIONS_WRITE,
    SHOPS_READ, SHOPS_WRITE,
    CATEGORY_CHANGES_READ, CATEGORY_CHANGES_WRITE,

    ORDERS_READ, ORDERS_WRITE,
    RETURNS_READ, RETURNS_WRITE,

    COURIERS_READ, COURIERS_WRITE,
    COURIER_EARNINGS_READ,
    DELIVERIES_READ, DELIVERIES_WRITE,
    DELIVERY_PRICING_READ,
    /** Attribuee au cas par cas a l'Administrateur des livraisons (case sur son compte). */
    DELIVERY_PRICING_WRITE,

    /** Registre des revenus de la plateforme (Super Admin et Auditeur uniquement). */
    REVENUE_READ,

    PAYOUTS_READ,
    /** Recevoir, verifier, renvoyer pour correction et soumettre un retrait (jamais l'approuver). */
    PAYOUTS_PREPARE,
    /** Approbation finale, rejet, paiement : Super Admin uniquement. */
    PAYOUTS_APPROVE,

    SUPPORT_READ, SUPPORT_WRITE,

    CONTENT_READ, CONTENT_WRITE,

    USERS_READ, USERS_WRITE,
    AUDIT_READ,
    /** Comptes du personnel : creation, roles, activation, reinitialisation. */
    STAFF_MANAGE
}
