package com.maplenou.support.identity.domain;

import com.maplenou.support.shared.Role;

/**
 * Maps a Maplenou account to a support role (DEC-CS10-17). Maplenou has one account per person: buyer by default,
 * {@code role = ADMIN} or {@code DELIVERY_AGENT}, seller when the seller profile is APPROVED.
 * A support role given by an administrator (support_staff_assignments) wins.
 */
public final class MaplenouRoles {

    private MaplenouRoles() {
    }

    public static Role of(String coreRole, String sellerStatus, Role staffRole) {
        if (staffRole != null) {
            return staffRole;
        }
        if ("ADMIN".equals(coreRole)) {
            return Role.ADMINISTRATOR;
        }
        if ("SUPPORT_AGENT".equals(coreRole)) {
            // Role "Agent support et retraits" of the Maplenou back-office staff (Bakend RolePermissions).
            return Role.SUPPORT_AGENT;
        }
        if ("DELIVERY_AGENT".equals(coreRole)) {
            return Role.DELIVERY_DRIVER;
        }
        if ("APPROVED".equals(sellerStatus)) {
            return Role.SELLER;
        }
        return Role.CUSTOMER;
    }

    /** Same phone format as the core (digits with an optional leading +), so assignments match sign-ins. */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim().replaceAll("[\\s.-]", "");
        return trimmed.matches("^\\+?[0-9]{8,15}$") ? trimmed : null;
    }
}
