package com.maplenou.backend.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, UUID> {

    List<DeliveryZone> findByActiveTrueOrderByNameAsc();

    Optional<DeliveryZone> findByNameIgnoreCase(String name);
}
