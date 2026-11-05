package com.maplenou.backend.order.dto;

import com.maplenou.backend.order.SubOrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record SubOrderItemResponse(
        UUID id,
        UUID productVariantId,   // peut être null si la variante a été supprimée
        String productName,
        String variantLabel,
        String sku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        BigDecimal baseUnitPrice   // prix de base vendeur (unitPrice = prix affiche commission incluse)
) {
    public static SubOrderItemResponse from(SubOrderItem item) {
        return new SubOrderItemResponse(
                item.getId(),
                item.getProductVariant() != null ? item.getProductVariant().getId() : null,
                item.getSnapshotProductName(),
                item.getSnapshotVariantLabel(),
                item.getSnapshotSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getLineTotal(),
                item.getBaseUnitPrice()
        );
    }
}
