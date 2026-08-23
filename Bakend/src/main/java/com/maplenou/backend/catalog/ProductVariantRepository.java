package com.maplenou.backend.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductId(UUID productId);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    boolean existsBySku(String sku);

    /**
     * Décrémentation atomique du stock.
     * Retourne 1 si succès, 0 si stock insuffisant (stock < qty).
     * Appelé uniquement lors de la transition PAID (webhook paiement).
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty " +
           "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);

    /**
     * Incrémentation du stock lors d'un retour approuvé.
     * Appelé uniquement quand ReturnRequest.decision = APPROVED.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty WHERE v.id = :id")
    void incrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
