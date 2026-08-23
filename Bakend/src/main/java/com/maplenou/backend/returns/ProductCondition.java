package com.maplenou.backend.returns;

public enum ProductCondition {
    NEW,     // Produit non ouvert, état neuf
    GOOD,    // Produit ouvert mais en bon état
    DAMAGED  // Produit endommagé (description dans la raison)
}
