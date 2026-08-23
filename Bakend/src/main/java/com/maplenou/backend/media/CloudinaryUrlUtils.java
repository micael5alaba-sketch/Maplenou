package com.maplenou.backend.media;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extrait le public_id Cloudinary d'une URL de livraison standard, pour permettre sa suppression. */
public final class CloudinaryUrlUtils {

    private static final Pattern UPLOAD_PATTERN =
            Pattern.compile(".*/upload/(?:v\\d+/)?(.+?)(?:\\.[a-zA-Z0-9]+)?$");

    private CloudinaryUrlUtils() {
    }

    /** Retourne le public_id (ex: "products/abc123") ou null si l'URL ne suit pas le format Cloudinary. */
    public static String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("res.cloudinary.com")) {
            return null;
        }
        Matcher matcher = UPLOAD_PATTERN.matcher(cloudinaryUrl);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
