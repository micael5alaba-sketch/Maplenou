package com.maplenou.backend.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierDocumentRepository extends JpaRepository<CourierDocument, UUID> {

    List<CourierDocument> findByCourierId(UUID courierId);

    Optional<CourierDocument> findByCourierIdAndType(UUID courierId, CourierDocumentType type);

    @EntityGraph(attributePaths = {"courier"})
    Page<CourierDocument> findByStatusOrderBySubmittedAtAsc(CourierDocumentStatus status, Pageable pageable);
}
