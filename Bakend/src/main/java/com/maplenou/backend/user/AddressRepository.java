package com.maplenou.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserId(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("DELETE FROM Address a WHERE a.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
