package com.maplenou.support.shared;

/** Small string helpers shared by the use cases. */
public final class Text {

    private Text() {
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
