package com.maplenou.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {

    Optional<MfaRecoveryCode> findByUserIdAndCodeHashAndUsedAtIsNull(UUID userId, String codeHash);

    long countByUserIdAndUsedAtIsNull(UUID userId);

    @Modifying
    @Query("DELETE FROM MfaRecoveryCode c WHERE c.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
