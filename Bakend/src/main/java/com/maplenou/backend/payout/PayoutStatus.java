package com.maplenou.backend.payout;

/**
 * Statuts d'un retrait (vendeur ou livreur). Circuit obligatoire (document "Rôles et accès au
 * tableau de bord", §8) :
 * PENDING → PREPARED → SUBMITTED → APPROVED → (PROCESSING) → PAID, avec les branches
 * NEEDS_CORRECTION, REJECTED et FAILED. Voir {@code WithdrawalWorkflow}.
 */
public enum PayoutStatus {
    /** Reçu (généré ou demandé), en attente de vérification par l'Agent support et retraits. */
    PENDING,
    /** Vérifié et préparé par l'Agent support (données, solde, identité, justificatifs contrôlés). */
    PREPARED,
    /** Dossier soumis au Super Admin pour décision. */
    SUBMITTED,
    /** Renvoyé pour correction par l'Agent support (donnée ou justificatif à rectifier). */
    NEEDS_CORRECTION,
    /** Approuvé par le Super Admin (MFA vérifiée) : seul statut à partir duquel un retrait est payable. */
    APPROVED,
    /** Rejeté par le Super Admin (justification obligatoire). */
    REJECTED,
    /** En cours de virement (après approbation). */
    PROCESSING,
    /** Virement effectué avec succès. */
    PAID,
    /** Échec du virement (à retraiter par le Super Admin). */
    FAILED
}
