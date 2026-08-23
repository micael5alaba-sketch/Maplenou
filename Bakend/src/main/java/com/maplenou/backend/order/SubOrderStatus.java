package com.maplenou.backend.order;

public enum SubOrderStatus {
    PENDING,            // Commande reçue, en attente de traitement vendeur
    PREPARING,          // Vendeur prépare le colis
    READY_FOR_PICKUP,   // Prêt à être récupéré par le livreur
    IN_DELIVERY,        // Livreur en route vers le client
    DELIVERED,          // Livré et confirmé avec preuve
    CANCELLED,          // Annulée
    RETURN_REQUESTED,   // Client a demandé un retour
    RETURNED            // Retour accepté et traité
}
