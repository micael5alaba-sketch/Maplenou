package com.maplenou.backend.catalog;

/**
 * Paire libre label/valeur affichee sur la fiche produit (Marque, Matiere, Origine, Pointure...).
 * Stockee en JSONB sur Product.specifications, sans champs predefinis : chaque vendeur y met ce
 * qui est pertinent pour son produit.
 */
public record ProductSpecification(String label, String value) {
}
