package com.maplenou.backend.messaging;

import java.util.regex.Pattern;

/**
 * Détection des coordonnées personnelles dans les messages client↔vendeur (§9.3 du cahier des
 * charges) : empêche les transactions hors plateforme en bloquant les numéros de téléphone,
 * emails et mentions de réseaux sociaux/messageries externes.
 */
public final class PersonalDataFilter {

    private static final Pattern PHONE = Pattern.compile("(\\+?\\d[\\s.\\-]?){7,}\\d");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}");
    private static final Pattern SOCIAL_KEYWORDS = Pattern.compile(
            "(?i)\\b(whatsapp|whats app|telegram|instagram|facebook|messenger|tiktok|snapchat|imo)\\b|@[a-zA-Z0-9._]{3,}"
    );

    private PersonalDataFilter() {
    }

    public static boolean containsPersonalData(String text) {
        if (text == null || text.isBlank()) return false;
        return PHONE.matcher(text).find()
                || EMAIL.matcher(text).find()
                || SOCIAL_KEYWORDS.matcher(text).find();
    }
}
