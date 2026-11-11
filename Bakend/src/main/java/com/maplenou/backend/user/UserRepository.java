package com.maplenou.backend.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    // ----- KPI queries -----

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countCreatedSince(@Param("since") Instant since);

    // ----- Admin queries -----

    @Query(value = "SELECT u FROM User u WHERE u.role IN (com.maplenou.backend.user.Role.ADMIN, "
            + "com.maplenou.backend.user.Role.DELIVERY_ADMIN, com.maplenou.backend.user.Role.SUPPORT_AGENT, "
            + "com.maplenou.backend.user.Role.AUDITOR) AND (:active IS NULL OR u.active = :active) ORDER BY u.createdAt DESC",
            countQuery = "SELECT COUNT(u) FROM User u WHERE u.role IN (com.maplenou.backend.user.Role.ADMIN, "
                    + "com.maplenou.backend.user.Role.DELIVERY_ADMIN, com.maplenou.backend.user.Role.SUPPORT_AGENT, "
                    + "com.maplenou.backend.user.Role.AUDITOR) AND (:active IS NULL OR u.active = :active)")
    Page<User> findStaff(@Param("active") Boolean active, Pageable pageable);

    long countByRoleAndActiveTrue(Role role);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:active IS NULL OR u.active = :active)")
    Page<User> findByFilters(@Param("role") Role role, @Param("active") Boolean active, Pageable pageable);
}
