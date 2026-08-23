package com.maplenou.backend.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommissionServiceTest {

    private final CommissionService service = new CommissionService();

    @Test
    void appliesHighRateAtThreshold() {
        // 20 000 FCFA pile => taux bas (<=)
        assertEquals(0, new BigDecimal("0.2500").compareTo(service.rateFor(new BigDecimal("20000"))));
    }

    @Test
    void appliesLowRateBelowThreshold() {
        assertEquals(0, new BigDecimal("0.2500").compareTo(service.rateFor(new BigDecimal("10000"))));
    }

    @Test
    void appliesReducedRateAboveThreshold() {
        assertEquals(0, new BigDecimal("0.2250").compareTo(service.rateFor(new BigDecimal("20000.01"))));
    }

    @Test
    void computesCommissionAmountBelowThreshold() {
        // 10 000 * 25% = 2 500.00
        assertEquals(0, new BigDecimal("2500.00").compareTo(service.amountFor(new BigDecimal("10000"))));
    }

    @Test
    void computesCommissionAmountAboveThreshold() {
        // 30 000 * 22.5% = 6 750.00
        assertEquals(0, new BigDecimal("6750.00").compareTo(service.amountFor(new BigDecimal("30000"))));
    }

    @Test
    void netAmountIsSubtotalMinusCommission() {
        BigDecimal subtotal = new BigDecimal("10000");
        BigDecimal expectedNet = subtotal.subtract(service.amountFor(subtotal));
        assertEquals(0, expectedNet.compareTo(service.netFor(subtotal)));
    }
}
