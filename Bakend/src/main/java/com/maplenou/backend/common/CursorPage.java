package com.maplenou.backend.common;

import java.util.List;

/**
 * Réponse paginée par curseur (keyset pagination).
 * Utilisée pour tout endpoint de catalogue public à fort volume.
 * nextCursor est null quand on est sur la dernière page.
 */
public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        boolean hasNext
) {
}
