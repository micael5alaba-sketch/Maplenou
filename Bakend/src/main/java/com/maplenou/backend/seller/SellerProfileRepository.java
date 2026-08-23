package com.maplenou.backend.seller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, UUID> {

    Optional<SellerProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<SellerProfile> findByStatus(SellerStatus status, Pageable pageable);
}
