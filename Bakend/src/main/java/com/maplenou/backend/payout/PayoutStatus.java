package com.maplenou.backend.payout;

public enum PayoutStatus {
    /** Généré, en attente de traitement par l'admin. */
    PENDING,
    /** En cours de virement (traitement en cours). */
    PROCESSING,
    /** Virement effectué avec succès. */
    PAID,
    /** Échec du virement (à retraiter). */
    FAILED
}
