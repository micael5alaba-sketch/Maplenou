package com.maplenou.backend.returns;

public enum RefundStatus {
    PENDING,    // En attente de décision
    PROCESSING, // Approuvé — remboursement en cours de traitement
    REFUNDED,   // Remboursé (à confirmer manuellement ou via webhook)
    DENIED      // Retour rejeté, pas de remboursement
}
