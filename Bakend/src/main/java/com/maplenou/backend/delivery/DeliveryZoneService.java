package com.maplenou.backend.delivery;

import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.CreateDeliveryZoneRequest;
import com.maplenou.backend.delivery.dto.DeliveryZoneResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryZoneService {

    private final DeliveryZoneRepository zoneRepository;

    @Cacheable("delivery-zones")
    @Transactional(readOnly = true)
    public List<DeliveryZoneResponse> listActive() {
        return zoneRepository.findByActiveTrueOrderByNameAsc()
                .stream().map(DeliveryZoneResponse::from).toList();
    }

    @CacheEvict(value = "delivery-zones", allEntries = true)
    @Transactional
    public DeliveryZoneResponse create(CreateDeliveryZoneRequest request) {
        zoneRepository.findByNameIgnoreCase(request.name()).ifPresent(z -> {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Une zone \"" + request.name() + "\" existe déjà");
        });

        DeliveryZone zone = DeliveryZone.builder()
                .name(request.name())
                .deliveryFee(request.deliveryFee())
                .estimatedTimeMinutes(request.estimatedTimeMinutes())
                .build();

        return DeliveryZoneResponse.from(zoneRepository.save(zone));
    }

    @CacheEvict(value = "delivery-zones", allEntries = true)
    @Transactional
    public DeliveryZoneResponse toggleActive(UUID id) {
        DeliveryZone zone = zoneRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Zone introuvable"));
        zone.setActive(!zone.isActive());
        return DeliveryZoneResponse.from(zoneRepository.save(zone));
    }

    @CacheEvict(value = "delivery-zones", allEntries = true)
    @Transactional
    public void delete(UUID id) {
        if (!zoneRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Zone introuvable");
        }
        zoneRepository.deleteById(id);
    }
}
