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

    /**
     * Relit le stock courant après un {@link #decrementStock} dans la même transaction — une mise
     * à jour en masse (@Modifying) ne rafraîchit pas automatiquement l'entité déjà chargée en
     * mémoire, donc on ne peut pas se fier à {@code variant.getStockQuantity()} juste après.
     * Utilisé par PaymentWebhookService pour détecter le franchissement du seuil de stock bas.
     */
    @Query("SELECT v.stockQuantity FROM ProductVariant v WHERE v.id = :id")
    Integer findStockQuantityById(@Param("id") UUID id);
}
