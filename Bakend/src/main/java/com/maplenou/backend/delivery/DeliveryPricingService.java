package com.maplenou.backend.delivery;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.delivery.dto.DeliveryFeeQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tarification de la livraison par paliers de distance réelle (décision porteur de projet,
 * revue du 18/10/2026 : plus de "zones" nommées à faire correspondre à une ville/quartier —
 * uniquement la distance à vol d'oiseau entre la boutique et l'adresse de livraison, voir
 * {@link GeoUtils}, pas d'API de cartographie externe). L'admin configure un {@link
 * DeliveryPricingTier} par tranche ("jusqu'à X km, prix Y") via {@link DeliveryPricingTierService} ;
 * ce service se contente de calculer la distance réelle et de trouver le palier qui s'applique.
 *
 * Remplace l'ancienne logique zone + prix/km (tarif de base + supplément au km), elle-même
 * un remplacement de la correspondance approximative de ville/quartier d'avant. Calculé une
 * seule fois au moment de la commande et figé sur le SubOrder.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPricingService {

    /** Repli si la boutique ou l'adresse n'a pas (encore) de coordonnées GPS : pas de
     * calcul de distance possible, mieux vaut un tarif par défaut que bloquer le checkout. */
    private static final BigDecimal DEFAULT_FEE = new BigDecimal("1000.00");

    private final DeliveryPricingTierRepository tierRepository;

    public DeliveryFeeQuote quote(Shop shop, BigDecimal destLatitude, BigDecimal destLongitude) {
        if (shop.getLatitude() == null || shop.getLongitude() == null
                || destLatitude == null || destLongitude == null) {
            return new DeliveryFeeQuote(DEFAULT_FEE, null);
        }

        double distanceKmRaw = GeoUtils.haversineKm(
                shop.getLatitude().doubleValue(), shop.getLongitude().doubleValue(),
                destLatitude.doubleValue(), destLongitude.doubleValue());
        BigDecimal distanceKm = BigDecimal.valueOf(distanceKmRaw).setScale(2, RoundingMode.HALF_UP);

        List<DeliveryPricingTier> tiers = tierRepository.findAllOrderedByMaxDistanceKmAscNullsLast();
        BigDecimal fee = tiers.stream()
                .filter(t -> t.getMaxDistanceKm() == null || distanceKm.compareTo(t.getMaxDistanceKm()) <= 0)
                .findFirst()
                .map(DeliveryPricingTier::getPrice)
                .orElse(DEFAULT_FEE);

        return new DeliveryFeeQuote(fee, distanceKm);
    }
}
