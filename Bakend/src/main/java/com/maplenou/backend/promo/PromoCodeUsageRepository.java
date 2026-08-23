package com.maplenou.backend.promo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, UUID> {

    /** Vérifie si un utilisateur a déjà utilisé ce code. */
    boolean existsByPromoCodeIdAndUserId(UUID promoCodeId, UUID userId);

    /** Vérifie si ce code a déjà été appliqué à cette commande. */
    boolean existsByPromoCodeIdAndOrderId(UUID promoCodeId, UUID orderId);
}
