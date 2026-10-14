package com.maplenou.backend.sellerfee;

public enum SellerMonthlyFeeStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    /** Annulé parce que la seule vente éligible du mois a finalement été retournée. */
    CANCELLED
}
