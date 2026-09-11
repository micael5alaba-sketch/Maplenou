package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.OrderStatus;

import java.util.Map;

/**
 * Compteur de commandes de l'utilisateur connecté, par statut.
 * "ongoing" = commandes créées ou payées, pas encore closes/annulées/remboursées.
 */
public record OrdersSummaryResponse(
        long total,
        long ongoing,
        Map<OrderStatus, Long> byStatus
) {
}
