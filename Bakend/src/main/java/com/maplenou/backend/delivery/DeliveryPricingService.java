package com.maplenou.backend.delivery;

import com.maplenou.backend.catalog.Shop;
import com.maplenou.backend.delivery.dto.DeliveryFeeQuote;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Tarification dynamique de la livraison (décision porteur de projet, type Gozem) :
 * tarif de base de la {@link DeliveryZone} correspondant à l'adresse de livraison, plus
 * un supplément au kilomètre calculé sur la distance réelle boutique → adresse (à vol
 * d'oiseau, voir {@link GeoUtils} — pas d'API de cartographie externe).
 *
 * Remplace l'ancienne logique de {@code CourierEarningService.resolveFee} qui déduisait
 * le tarif après coup par correspondance approximative de ville/quartier, l'Order ne
 * mémorisant alors aucun frais de livraison réel (jamais facturé à l'acheteur non plus).
 * Désormais calculé une seule fois au moment de la commande et figé sur le SubOrder.
 */
@Service
@RequiredArgsConstructor
public class DeliveryPricingService {

    private static final BigDecimal DEFAULT_FEE = new BigDecimal("1000.00");

    private final DeliveryZoneRepository deliveryZoneRepository;

    public DeliveryFeeQuote quote(Shop shop, String city, String district,
                                   BigDecimal destLatitude, BigDecimal destLongitude) {
        DeliveryZone zone = resolveZone(city, district);

        BigDecimal baseFee = zone != null ? zone.getDeliveryFee() : DEFAULT_FEE;
        BigDecimal pricePerKm = zone != null ? zone.getPricePerKm() : BigDecimal.ZERO;
        String zoneName = zone != null ? zone.getName() : null;

        if (shop.getLatitude() == null || shop.getLongitude() == null
                || destLatitude == null || destLongitude == null
                || pricePerKm.signum() <= 0) {
            // Coordonnées manquantes (boutique ou adresse pas encore géolocalisée) ou zone
            // sans supplément au km configuré : tarif de base seul, pas de surprise pour
            // l'acheteur plutôt qu'une erreur qui bloquerait le checkout.
            return new DeliveryFeeQuote(baseFee, null, zoneName);
        }

        double distanceKmRaw = GeoUtils.haversineKm(
                shop.getLatitude().doubleValue(), shop.getLongitude().doubleValue(),
                destLatitude.doubleValue(), destLongitude.doubleValue());
        BigDecimal distanceKm = BigDecimal.valueOf(distanceKmRaw).setScale(2, RoundingMode.HALF_UP);

        BigDecimal fee = baseFee.add(pricePerKm.multiply(distanceKm)).setScale(2, RoundingMode.HALF_UP);
        return new DeliveryFeeQuote(fee, distanceKm, zoneName);
    }

    private DeliveryZone resolveZone(String city, String district) {
        Optional<DeliveryZone> zone = Optional.<DeliveryZone>empty()
                .or(() -> city != null ? deliveryZoneRepository.findByNameIgnoreCase(city) : Optional.empty())
                .or(() -> district != null ? deliveryZoneRepository.findByNameIgnoreCase(district) : Optional.empty());

        if (zone.isPresent()) {
            return zone.get();
        }

        List<DeliveryZone> activeZones = deliveryZoneRepository.findByActiveTrueOrderByNameAsc();
        return activeZones.stream()
                .filter(z -> z.getName().equalsIgnoreCase("Grand Lomé"))
                .findFirst()
                .or(() -> activeZones.stream().findFirst())
                .orElse(null);
    }
}
