package com.maplenou.backend.delivery;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.CreateDeliveryPricingTierRequest;
import com.maplenou.backend.delivery.dto.DeliveryPricingTierResponse;
import com.maplenou.backend.delivery.dto.UpdateDeliveryPricingTierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD admin du barème de livraison par paliers de distance (voir {@link DeliveryPricingTier}). */
@Service
@RequiredArgsConstructor
public class DeliveryPricingTierService {

    private final DeliveryPricingTierRepository tierRepository;

    @Cacheable("delivery-pricing-tiers")
    @Transactional(readOnly = true)
    public List<DeliveryPricingTierResponse> list() {
        return tierRepository.findAllOrderedByMaxDistanceKmAscNullsLast()
                .stream().map(DeliveryPricingTierResponse::from).toList();
    }

    @CacheEvict(value = "delivery-pricing-tiers", allEntries = true)
    @Transactional
    public DeliveryPricingTierResponse create(CreateDeliveryPricingTierRequest request) {
        if (request.maxDistanceKm() == null && tierRepository.existsByMaxDistanceKmIsNull()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Un palier \"au-delà de X km\" (illimité) existe déjà — modifie-le plutôt que d'en créer un second");
        }

        DeliveryPricingTier tier = DeliveryPricingTier.builder()
                .maxDistanceKm(request.maxDistanceKm())
                .price(request.price())
                .build();

        return DeliveryPricingTierResponse.from(tierRepository.save(tier));
    }

    @CacheEvict(value = "delivery-pricing-tiers", allEntries = true)
    @Transactional
    public DeliveryPricingTierResponse update(UUID id, UpdateDeliveryPricingTierRequest request) {
        DeliveryPricingTier tier = tierRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Palier introuvable"));

        if (request.maxDistanceKm() == null && tierRepository.existsByMaxDistanceKmIsNullAndIdNot(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Un palier \"au-delà de X km\" (illimité) existe déjà — un seul autorisé");
        }

        tier.setMaxDistanceKm(request.maxDistanceKm());
        tier.setPrice(request.price());

        return DeliveryPricingTierResponse.from(tierRepository.save(tier));
    }

    @CacheEvict(value = "delivery-pricing-tiers", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!tierRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Palier introuvable");
        }
        tierRepository.deleteById(id);
    }
}
