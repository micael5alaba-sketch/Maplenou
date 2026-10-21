package com.maplenou.backend.auth.dto;

/**
 * Réponse strictement identique quel que soit le résultat réel (numéro inconnu, compte sans
 * email, compte éligible, échec d'envoi) — correctif F-01 (pentest du 21/10/2026) : avant ce
 * changement, /api/auth/forgot-password distinguait ces cas par son code HTTP, permettant de
 * déterminer quels numéros de téléphone possèdent un compte.
 */
public record ForgotPasswordResponse(String message) {

    private static final String GENERIC_MESSAGE =
            "Si un compte éligible correspond à ces informations, les instructions de réinitialisation ont été envoyées.";

    public static ForgotPasswordResponse generic() {
        return new ForgotPasswordResponse(GENERIC_MESSAGE);
    }
}
