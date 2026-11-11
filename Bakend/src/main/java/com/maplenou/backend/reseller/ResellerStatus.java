package com.maplenou.backend.reseller;

/** Statuts du profil partenaire (transitions controlees et journalisees, voir ResellerProfileService). */
public enum ResellerStatus {
    PENDING, APPROVED, REJECTED, SUSPENDED, DEACTIVATED
}
