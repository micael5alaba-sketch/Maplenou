package com.maplenou.backend.delivery;

public enum DeliveryStatus {
    PENDING,      // Livraison créée, en attente d'affectation
    ASSIGNED,     // Livreur assigné
    IN_TRANSIT,   // En cours de livraison
    DELIVERED,    // Livraison confirmée avec preuve
    FAILED        // Échec (client absent, adresse introuvable…)
}
