package com.maplenou.backend.reseller;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcul pur (sans base de données) de la commission d'une unité vendue via un partenaire.
 * Règles du programme : le prix unitaire final réellement payé détermine le taux Maplenou
 * (jusqu'au seuil inclus : taux bas ; au-dessus : taux haut), puis le partenaire reçoit sa part de
 * CETTE commission. Arrondi au FCFA entier (demi vers le haut) à chaque étape, par unité, de sorte
 * que la part partenaire ne dépasse jamais la commission réelle de Maplenou.
 */
public final class ResellerCommissionCalculator {

    private ResellerCommissionCalculator() {
    }

    /** Résultat figé d'une unité : taux appliqué, commission Maplenou, part partenaire, reste Maplenou. */
    public record Result(BigDecimal rate, long maplenouCommission, long partnerShare, long maplenouShare) {
    }

    public static boolean isEligible(long unitPrice, ResellerSettings s) {
        return unitPrice >= s.getMinUnitPrice();
    }

    public static BigDecimal rateFor(long unitPrice, ResellerSettings s) {
        return unitPrice <= s.getTierThreshold() ? s.getRateLow() : s.getRateHigh();
    }

    /** @throws IllegalArgumentException si le prix est inférieur au minimum éligible */
    public static Result compute(long unitPrice, ResellerSettings s) {
        if (!isEligible(unitPrice, s)) {
            throw new IllegalArgumentException("Prix inférieur au minimum éligible : " + unitPrice);
        }
        BigDecimal rate = rateFor(unitPrice, s);
        long commission = BigDecimal.valueOf(unitPrice).multiply(rate).setScale(0, RoundingMode.HALF_UP).longValueExact();
        long partner = BigDecimal.valueOf(commission).multiply(s.getShareRate()).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new Result(rate, commission, partner, commission - partner);
    }
}
