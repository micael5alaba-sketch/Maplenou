package com.maplenou.backend.reseller;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Exemples chiffrés du guide du programme partenaire (version 2.0) : taux par palier, arrondi par unité. */
class ResellerCommissionCalculatorTest {

    private static ResellerSettings defaults() {
        return ResellerSettings.builder().version(1).shareRate(new BigDecimal("0.25")).minUnitPrice(2500)
                .tierThreshold(10000).rateLow(new BigDecimal("0.05")).rateHigh(new BigDecimal("0.03"))
                .attributionDays(30).minWithdrawal(3000).withdrawalFeeRate(new BigDecimal("0.03"))
                .programActive(true).build();
    }

    private static void check(long price, long commission, long partner) {
        var r = ResellerCommissionCalculator.compute(price, defaults());
        assertEquals(commission, r.maplenouCommission(), "commission pour " + price);
        assertEquals(partner, r.partnerShare(), "part partenaire pour " + price);
        assertEquals(commission - partner, r.maplenouShare());
        assertTrue(r.partnerShare() <= r.maplenouCommission());
    }

    @Test
    void documentExamples() {
        check(2500, 125, 31);
        check(3000, 150, 38);
        check(5000, 250, 63);
        check(10000, 500, 125);
        check(15000, 450, 113);
    }

    @Test
    void tierBoundaryIsInclusiveInLowTier() {
        assertEquals(0, new BigDecimal("0.05").compareTo(ResellerCommissionCalculator.rateFor(10000, defaults())));
        assertEquals(0, new BigDecimal("0.03").compareTo(ResellerCommissionCalculator.rateFor(10001, defaults())));
    }

    @Test
    void below2500IsIneligible() {
        assertFalse(ResellerCommissionCalculator.isEligible(2499, defaults()));
        assertTrue(ResellerCommissionCalculator.isEligible(2500, defaults()));
        assertThrows(IllegalArgumentException.class, () -> ResellerCommissionCalculator.compute(2499, defaults()));
    }
}
