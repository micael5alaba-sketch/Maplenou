package com.maplenou.backend.order;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul de la commission Maplenou sur le sous-total produits (hors livraison).
 *
 * Barème :
 *   subtotal ≤ 20 000 FCFA  → 25,00 %  (taux = 0.2500)
 *   subtotal > 20 000 FCFA  → 22,50 %  (taux = 0.2250)
 */
@Service
public class CommissionService {

    private static final BigDecimal THRESHOLD  = new BigDecimal("20000");
    private static final BigDecimal RATE_LOW   = new BigDecimal("0.2500");
    private static final BigDecimal RATE_HIGH  = new BigDecimal("0.2250");

    public BigDecimal rateFor(BigDecimal subtotal) {
        return subtotal.compareTo(THRESHOLD) <= 0 ? RATE_LOW : RATE_HIGH;
    }

    public BigDecimal amountFor(BigDecimal subtotal) {
        return subtotal.multiply(rateFor(subtotal)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal netFor(BigDecimal subtotal) {
        return subtotal.subtract(amountFor(subtotal));
    }
}
