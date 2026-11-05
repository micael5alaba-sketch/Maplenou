package com.maplenou.backend.order;

import com.maplenou.backend.seller.SellerFeeCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Commission integree au prix affiche : 5 % (Standard) / 3 % (Prix eleves) sur le prix de base. */
class CommissionServiceTest {

    @Test
    void standardCategoryRateIsFivePercent() {
        assertEquals(0, new BigDecimal("0.0500").compareTo(CommissionService.rateFor(SellerFeeCategory.STANDARD)));
    }

    @Test
    void highPriceCategoryRateIsThreePercent() {
        assertEquals(0, new BigDecimal("0.0300").compareTo(CommissionService.rateFor(SellerFeeCategory.HIGH_PRICE)));
    }

    @Test
    void documentExampleStandard() {
        // Exemple du document : base 10 000, commission 500, prix affiche 10 500
        BigDecimal base = new BigDecimal("10000");
        assertEquals(0, new BigDecimal("500").compareTo(CommissionService.commissionPerUnit(base, SellerFeeCategory.STANDARD)));
        assertEquals(0, new BigDecimal("10500").compareTo(CommissionService.displayPrice(base, SellerFeeCategory.STANDARD)));
    }

    @Test
    void highPriceCategoryExample() {
        // 30 000 * 3 % = 900 => 30 900
        BigDecimal base = new BigDecimal("30000");
        assertEquals(0, new BigDecimal("900").compareTo(CommissionService.commissionPerUnit(base, SellerFeeCategory.HIGH_PRICE)));
        assertEquals(0, new BigDecimal("30900").compareTo(CommissionService.displayPrice(base, SellerFeeCategory.HIGH_PRICE)));
    }

    @Test
    void commissionIsRoundedToWholeFcfaPerUnit() {
        // 1 250 * 5 % = 62,5 => 63 (HALF_UP) ; prix affiche 1 313 ; jamais de centimes de FCFA
        BigDecimal base = new BigDecimal("1250");
        assertEquals(0, new BigDecimal("63").compareTo(CommissionService.commissionPerUnit(base, SellerFeeCategory.STANDARD)));
        assertEquals(0, new BigDecimal("1313").compareTo(CommissionService.displayPrice(base, SellerFeeCategory.STANDARD)));
    }

    @Test
    void minimumPriceStillCarriesCommission() {
        // 500 FCFA (prix minimum de vente) * 5 % = 25
        assertEquals(0, new BigDecimal("525").compareTo(CommissionService.displayPrice(new BigDecimal("500"), SellerFeeCategory.STANDARD)));
    }
}
