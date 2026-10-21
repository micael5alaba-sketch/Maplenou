package com.maplenou.backend.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hachage SHA-256 partagé — utilisé partout où une valeur sensible à haute entropie (jeton JWT,
 * code de réinitialisation, IP pour corrélation d'audit) doit être stockée ou comparée sans
 * jamais persister la valeur brute. Pas de pepper HMAC : ces valeurs sont déjà signées/aléatoires
 * (jamais un secret bas-entropie type mot de passe), donc une attaque par dictionnaire/rainbow
 * table n'est pas le modèle de menace pertinent ici.
 */
public final class Sha256Utils {

    private Sha256Utils() {
    }

    public static String hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 introuvable", e);
        }
    }

    /** Comparaison en temps constant — évite qu'un timing attack révèle combien de caractères
     * d'un hash stocké correspondent à la valeur fournie. */
    public static boolean matches(String storedHex, String providedRawValue) {
        if (storedHex == null) {
            return false;
        }
        return MessageDigest.isEqual(
                storedHex.getBytes(StandardCharsets.UTF_8),
                hex(providedRawValue).getBytes(StandardCharsets.UTF_8));
    }
}
