package com.maplenou.backend.security;

import com.maplenou.backend.user.Role;

import java.util.EnumSet;
import java.util.Set;

import static com.maplenou.backend.security.Permission.*;

/**
 * Matrice centralisee et versionnee des droits par role (document "Roles et acces au tableau de
 * bord", section 7). UN SEUL endroit : toute modification de droits passe par ce fichier.
 * Principe du moindre privilege : un role ne recoit que ce qui est indispensable a sa mission.
 */
public final class RolePermissions {

    private RolePermissions() {
    }

    /** Super Admin : toutes les permissions. */
    private static final Set<Permission> SUPER_ADMIN = EnumSet.allOf(Permission.class);

    /** Administrateur des livraisons : livraisons, livreurs, documents, gains. Rien d'autre. */
    private static final Set<Permission> DELIVERY_ADMIN = EnumSet.of(
            DASHBOARD_VIEW,
            COURIERS_READ, COURIERS_WRITE,
            COURIER_EARNINGS_READ,
            DELIVERIES_READ, DELIVERIES_WRITE,
            DELIVERY_PRICING_READ);

    /** Agent support et retraits : support, vendeurs, retours, preparation des retraits (pas l'approbation). */
    private static final Set<Permission> SUPPORT_AGENT = EnumSet.of(
            DASHBOARD_VIEW,
            SELLER_APPLICATIONS_READ, SELLER_APPLICATIONS_WRITE,
            SHOPS_READ, SHOPS_WRITE,
            CATEGORY_CHANGES_READ, CATEGORY_CHANGES_WRITE,
            ORDERS_READ, ORDERS_WRITE,
            RETURNS_READ, RETURNS_WRITE,
            SUPPORT_READ, SUPPORT_WRITE,
            COURIER_EARNINGS_READ,
            DELIVERIES_READ,
            RESELLERS_READ, RESELLERS_WRITE,
            PAYOUTS_READ, PAYOUTS_PREPARE);

    /** Auditeur : lecture seule, aucune permission d'ecriture, de preparation ou d'approbation. */
    private static final Set<Permission> AUDITOR = EnumSet.of(
            DASHBOARD_VIEW,
            SELLER_APPLICATIONS_READ, SHOPS_READ, CATEGORY_CHANGES_READ,
            ORDERS_READ, RETURNS_READ,
            COURIERS_READ, COURIER_EARNINGS_READ, DELIVERIES_READ, DELIVERY_PRICING_READ,
            PAYOUTS_READ, REVENUE_READ, RESELLERS_READ,
            SUPPORT_READ,
            CONTENT_READ,
            USERS_READ, AUDIT_READ);

    public static Set<Permission> forRole(Role role) {
        if (role == null) {
            return EnumSet.noneOf(Permission.class);
        }
        return switch (role) {
            case ADMIN -> SUPER_ADMIN;
            case DELIVERY_ADMIN -> DELIVERY_ADMIN;
            case SUPPORT_AGENT -> SUPPORT_AGENT;
            case AUDITOR -> AUDITOR;
            case DELIVERY_AGENT -> EnumSet.noneOf(Permission.class);
        };
    }
}
