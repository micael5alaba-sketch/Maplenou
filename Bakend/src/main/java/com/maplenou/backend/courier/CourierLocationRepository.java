package com.maplenou.backend.courier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, UUID> {

    @Query(value = "SELECT l FROM CourierLocation l JOIN FETCH l.agent a WHERE a.active = true ORDER BY l.updatedAt DESC",
            countQuery = "SELECT COUNT(l) FROM CourierLocation l WHERE l.agent.active = true")
    Page<CourierLocation> findAllActiveWithAgent(Pageable pageable);
}
