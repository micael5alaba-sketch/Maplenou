package com.maplenou.backend.courier;

import com.maplenou.backend.courier.dto.CourierLocationResponse;
import com.maplenou.backend.courier.dto.UpdateCourierLocationRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Position des livreurs pour la carte de l'admin (affectation des livraisons). Position
 * poussée périodiquement par l'app du livreur tant qu'elle est ouverte — distincte du tracking
 * GPS temps réel d'une course (Firebase RTDB, par livraison).
 */
@Service
@RequiredArgsConstructor
public class CourierLocationService {

    private final CourierLocationRepository repository;

    @Transactional
    public void update(User agent, UpdateCourierLocationRequest request) {
        CourierLocation location = repository.findById(agent.getId())
                .orElseGet(() -> CourierLocation.builder().agentId(agent.getId()).build());
        location.setLatitude(request.latitude());
        location.setLongitude(request.longitude());
        location.setUpdatedAt(Instant.now());
        repository.save(location);
    }

    @Transactional(readOnly = true)
    public Page<CourierLocationResponse> listForAdmin(Pageable pageable) {
        return repository.findAllActiveWithAgent(pageable)
                .map(CourierLocationResponse::from);
    }
}
