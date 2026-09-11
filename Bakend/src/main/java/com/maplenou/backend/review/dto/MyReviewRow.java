package com.maplenou.backend.review.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Projection native pour la requête combinée (UNION) des avis produits et boutiques d'un
 * utilisateur — voir ProductReviewRepository.findMyReviews.
 */
public interface MyReviewRow {
    UUID getId();
    String getType();       // "PRODUCT" ou "SHOP"
    UUID getTargetId();      // id du produit ou de la boutique évalué
    String getTargetName();
    Integer getRating();
    String getComment();
    Instant getCreatedAt();
}
