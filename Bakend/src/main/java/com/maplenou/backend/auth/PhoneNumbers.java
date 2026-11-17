package com.maplenou.backend.auth;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Formes sous lesquelles un même numéro a pu être enregistré. Selon la version de l'application,
 * un compte a pu être créé avec l'indicatif ("+22890123456") ou sans ("90123456") : la connexion
 * doit retrouver le compte quelle que soit la forme tapée ou enregistrée, sans jamais confondre
 * deux numéros différents.
 */
final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Formes candidates, de la plus probable à la moins probable (la saisie exacte d'abord). */
    static Set<String> candidates(String input) {
        Set<String> out = new LinkedHashSet<>();
        if (input == null) {
            return out;
        }
        String typed = input.trim();
        if (typed.isEmpty()) {
            return out;
        }
        out.add(typed);

        String cleaned = typed.replaceAll("[ \\t\\-.()]", "");
        if (cleaned.isEmpty()) {
            return out;
        }
        out.add(cleaned);

        String digits = cleaned.startsWith("+") ? cleaned.substring(1)
                : cleaned.startsWith("00") ? cleaned.substring(2) : cleaned;
        if (!digits.matches("[0-9]+")) {
            return out;
        }
        out.add("+" + digits);
        out.add(digits);

        // Numéro national (Togo : 8 chiffres, Bénin : 10 chiffres) -> avec indicatif
        if (digits.length() == 8) {
            out.add("+228" + digits);
            out.add("228" + digits);
        } else if (digits.length() == 10) {
            out.add("+229" + digits);
            out.add("229" + digits);
        }
        // Numéro international -> forme nationale
        if (digits.length() == 11 && digits.startsWith("228")) {
            out.add(digits.substring(3));
        } else if (digits.length() == 13 && digits.startsWith("229")) {
            out.add(digits.substring(3));
        }
        return out;
    }
}
