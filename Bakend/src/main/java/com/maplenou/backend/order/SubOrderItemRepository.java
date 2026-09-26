package com.maplenou.backend.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SubOrderItemRepository extends JpaRepository<SubOrderItem, UUID> {

    /**
     * Produit le plus vendu d'une boutique, par quantité — groupé sur le
     * nom figé à la commande (`snapshotProductName`), pas sur le produit
     * lui-même : une variante peut être soft-supprimée après coup, le nom
     * figé reste toujours disponible.
     */
    @Query("""
            SELECT i.snapshotProductName AS productName, SUM(i.quantity) AS totalQuantity
            FROM SubOrderItem i
            WHERE i.subOrder.shop.id = :shopId
            GROUP BY i.snapshotProductName
            ORDER BY SUM(i.quantity) DESC
            """)
    List<BestSellingRow> findBestSellingByShopId(@Param("shopId") UUID shopId, Pageable pageable);

    interface BestSellingRow {
        String getProductName();
        Long getTotalQuantity();
    }
}
