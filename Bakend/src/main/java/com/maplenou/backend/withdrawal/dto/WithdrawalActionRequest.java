package com.maplenou.backend.withdrawal.dto;

/**
 * Corps commun des actions du circuit des retraits. Tous les champs sont optionnels au niveau du
 * format ; les règles (justification obligatoire pour un rejet, renvoi ou échec ; code MFA pour
 * l'approbation) sont appliquées par le workflow selon l'action.
 */
public record WithdrawalActionRequest(
        /** Observations / justification (obligatoire pour un rejet, un renvoi pour correction ou un échec). */
        String notes,
        /** Code MFA (TOTP à 6 chiffres ou code de récupération) : obligatoire pour approuver. */
        String mfaCode
) {
}
