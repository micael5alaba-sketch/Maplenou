package com.maplenou.backend.delivery;

public enum CourierEarningStatus {
    /** Livraison terminée, gain dû mais pas encore versé. */
    PENDING,
    /** Versé au livreur (constaté manuellement par un admin, comme les payouts vendeur). */
    PAID
}
