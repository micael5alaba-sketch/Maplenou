package com.maplenou.backend.reseller;

/** Cycle d'une commission : PENDING a la creation de la commande, CREDITED a la validation de la livraison, CANCELLED si annulee avant livraison. */
public enum ResellerCommissionStatus {
    PENDING, CREDITED, CANCELLED
}
