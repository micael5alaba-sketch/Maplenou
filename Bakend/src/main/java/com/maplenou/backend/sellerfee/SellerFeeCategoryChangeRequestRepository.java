package com.maplenou.backend.sellerfee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerFeeCategoryChangeRequestRepository extends JpaRepository<SellerFeeCategoryChangeRequest, UUID> {

    boolean existsBySellerProfileIdAndStatus(UUID sellerProfileId, SellerFeeCategoryChangeStatus status);

    Optional<SellerFeeCategoryChangeRequest> findFirstBySellerProfileIdAndStatus(
            UUID sellerProfileId, SellerFeeCategoryChangeStatus status);

    Page<SellerFeeCategoryChangeRequest> findByStatus(SellerFeeCategoryChangeStatus status, Pageable pageable);

    /** Changements approuvés mais pas encore appliqués pour CE vendeur, dont la date d'effet est passée. */
    List<SellerFeeCategoryChangeRequest> findBySellerProfileIdAndStatusAndAppliedFalseAndEffectiveFromLessThanEqual(
            UUID sellerProfileId, SellerFeeCategoryChangeStatus status, LocalDate today);
}
