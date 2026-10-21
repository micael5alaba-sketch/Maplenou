package com.maplenou.backend.common;

import java.util.regex.Pattern;

/**
 * Retire toute balise HTML d'un texte libre avant stockage — trouvé par l'audit de sécurité
 * externe du 16/10/2026 : une adresse acceptait et restituait {@code <script>alert(1)</script>}
 * tel quel. L'app Flutter dessine le texte sur un canvas (pas d'exécution possible côté mobile),
 * mais tout rendu HTML futur (back-office, email, reçu) exécuterait le script — on nettoie donc
 * à l'entrée, une bonne fois, plutôt que de compter sur chaque futur point d'affichage pour
 * échapper correctement.
 *
 * Volontairement radical (retire toute balise plutôt qu'une liste blanche) : aucun champ libre
 * de l'app (adresse, nom de produit/boutique, message, ticket, avis) n'a besoin de HTML.
 */
public final class HtmlSanitizer {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]*>");

    private HtmlSanitizer() {
    }

    /** {@code null} reste {@code null} (champ optionnel) ; une chaîne sans balise ressort inchangée. */
    public static String stripTags(String input) {
        if (input == null) {
            return null;
        }
        return TAG_PATTERN.matcher(input).replaceAll("");
    }
}
