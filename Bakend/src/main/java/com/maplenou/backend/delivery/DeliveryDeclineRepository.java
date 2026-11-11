package com.maplenou.backend.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DeliveryDeclineRepository extends JpaRepository<DeliveryDecline, UUID> {

    @Query("SELECT d.agent.id FROM DeliveryDecline d WHERE d.delivery.id = :deliveryId")
    List<UUID> findDeclinedAgentIds(@Param("deliveryId") UUID deliveryId);
}
