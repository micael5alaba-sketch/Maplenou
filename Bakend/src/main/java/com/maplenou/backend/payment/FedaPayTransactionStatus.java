package com.maplenou.backend.payment;

import java.util.Set;

/**
 * Statut d'une transaction FedaPay tel que relu directement via l'API (jamais déduit du seul
 * contenu du webhook). Valeurs confirmées via le SDK PHP officiel (`Transaction.php`,
 * github.com/fedapay/fedapay-php) : pending, approved, declined, canceled, transferred,
 * refunded, approved_partially_refunded, transferred_partially_refunded.
 */
public record FedaPayTransactionStatus(long transactionId, String status) {

    private static final Set<String> PAID = Set.of(
            "approved", "transferred", "refunded",
            "approved_partially_refunded", "transferred_partially_refunded"
    );

    private static final Set<String> FAILED = Set.of("declined", "canceled");

    public boolean isPaid() {
        return PAID.contains(status);
    }

    public boolean isFailed() {
        return FAILED.contains(status);
    }
}
