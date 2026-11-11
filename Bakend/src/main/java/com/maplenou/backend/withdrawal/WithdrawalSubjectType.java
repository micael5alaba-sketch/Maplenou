package com.maplenou.backend.withdrawal;

/** Nature du retrait suivi par le circuit commun (historique, audit). */
public enum WithdrawalSubjectType {
    /** Reversement d'une boutique (entité Payout). */
    SELLER_PAYOUT,
    /** Retrait des gains d'un livreur (entité CourierWithdrawal). */
    COURIER_WITHDRAWAL,
    /** Retrait du portefeuille d'un partenaire revendeur (entité ResellerWithdrawal). */
    PARTNER_WITHDRAWAL
}
