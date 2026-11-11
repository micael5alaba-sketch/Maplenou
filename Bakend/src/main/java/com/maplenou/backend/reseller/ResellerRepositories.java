package com.maplenou.backend.reseller;

import com.maplenou.backend.payout.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Dépôts du programme partenaire regroupés (même package que les entités package-private).

interface ResellerSettingsRepository extends JpaRepository<ResellerSettings, UUID> {
    Optional<ResellerSettings> findFirstByOrderByVersionDesc();

    Optional<ResellerSettings> findByVersion(int version);

    Page<ResellerSettings> findAllByOrderByVersionDesc(Pageable pageable);
}

interface ResellerProfileRepository extends JpaRepository<ResellerProfile, UUID> {
    @Query("select p from ResellerProfile p join fetch p.user where p.user.id = :userId")
    Optional<ResellerProfile> findByUserId(@Param("userId") UUID userId);

    @Query(value = "select p from ResellerProfile p join fetch p.user where (:status is null or p.status = :status)",
            countQuery = "select count(p) from ResellerProfile p where (:status is null or p.status = :status)")
    Page<ResellerProfile> findByOptionalStatus(@Param("status") ResellerStatus status, Pageable pageable);

    @Query("select p from ResellerProfile p join fetch p.user where p.id = :id")
    Optional<ResellerProfile> findByIdWithUser(@Param("id") UUID id);

    long countByStatus(ResellerStatus status);

    /** Verrou d'écriture : sérialise les opérations de portefeuille d'un même partenaire. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ResellerProfile p where p.id = :id")
    Optional<ResellerProfile> lockById(@Param("id") UUID id);
}

interface ResellerReferralLinkRepository extends JpaRepository<ResellerReferralLink, UUID> {
    Optional<ResellerReferralLink> findByPartnerIdAndProductId(UUID partnerId, UUID productId);

    @Query("select l from ResellerReferralLink l join fetch l.partner join fetch l.product where l.code = :code")
    Optional<ResellerReferralLink> findByCodeWithDetails(@Param("code") String code);

    boolean existsByCode(String code);

    @Query(value = "select l from ResellerReferralLink l join fetch l.product where l.partner.id = :partnerId",
            countQuery = "select count(l) from ResellerReferralLink l where l.partner.id = :partnerId")
    Page<ResellerReferralLink> findByPartner(@Param("partnerId") UUID partnerId, Pageable pageable);
}

interface ResellerClickRepository extends JpaRepository<ResellerClick, UUID> {
    boolean existsByLinkIdAndVisitorHashAndCountedTrueAndClickedAtAfter(UUID linkId, String visitorHash, Instant after);

    long countByPartnerIdAndCountedTrue(UUID partnerId);
}

interface ResellerAttributionRepository extends JpaRepository<ResellerAttribution, UUID> {
    Optional<ResellerAttribution> findByOrderItemId(UUID orderItemId);
}

interface ResellerCommissionRepository extends JpaRepository<ResellerCommission, UUID> {
    List<ResellerCommission> findByOrderItemIdIn(Collection<UUID> orderItemIds);

    List<ResellerCommission> findByOrderItemIdInAndStatus(Collection<UUID> orderItemIds, ResellerCommissionStatus status);

    Page<ResellerCommission> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId, Pageable pageable);

    Page<ResellerCommission> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("select coalesce(sum(c.partnerShare), 0) from ResellerCommission c where c.partnerId = :partnerId and c.status = :status")
    long sumShareByPartnerAndStatus(@Param("partnerId") UUID partnerId, @Param("status") ResellerCommissionStatus status);

    @Query("select coalesce(sum(c.partnerShare), 0) from ResellerCommission c where c.status = :status")
    long sumShareByStatus(@Param("status") ResellerCommissionStatus status);

    long countByPartnerIdAndStatus(UUID partnerId, ResellerCommissionStatus status);
}

interface ResellerWalletTransactionRepository extends JpaRepository<ResellerWalletTransaction, UUID> {
    boolean existsByIdempotencyKey(String key);

    Page<ResellerWalletTransaction> findByPartnerIdOrderByCreatedAtDesc(UUID partnerId, Pageable pageable);

    @Query("select coalesce(sum(t.availableDelta), 0) from ResellerWalletTransaction t where t.partnerId = :partnerId")
    long sumAvailable(@Param("partnerId") UUID partnerId);

    @Query("select coalesce(sum(t.reservedDelta), 0) from ResellerWalletTransaction t where t.partnerId = :partnerId")
    long sumReserved(@Param("partnerId") UUID partnerId);

    @Query("select coalesce(sum(t.feeAmount), 0) from ResellerWalletTransaction t where t.partnerId = :partnerId")
    long sumFees(@Param("partnerId") UUID partnerId);

    @Query("select coalesce(sum(t.amount), 0) from ResellerWalletTransaction t where t.partnerId = :partnerId and t.type = :type")
    long sumAmountByType(@Param("partnerId") UUID partnerId, @Param("type") WalletTxType type);
}

interface ResellerWithdrawalRepository extends JpaRepository<ResellerWithdrawal, UUID> {
    @Query(value = "select w from ResellerWithdrawal w join fetch w.partner where w.partner.id = :partnerId order by w.createdAt desc",
            countQuery = "select count(w) from ResellerWithdrawal w where w.partner.id = :partnerId")
    Page<ResellerWithdrawal> findByPartner(@Param("partnerId") UUID partnerId, Pageable pageable);

    @Query(value = "select w from ResellerWithdrawal w join fetch w.partner where (:status is null or w.status = :status) order by w.createdAt desc",
            countQuery = "select count(w) from ResellerWithdrawal w where (:status is null or w.status = :status)")
    Page<ResellerWithdrawal> findByOptionalStatus(@Param("status") PayoutStatus status, Pageable pageable);

    @Query("select w from ResellerWithdrawal w join fetch w.partner where w.partner.id = :partnerId and w.idempotencyKey = :key")
    Optional<ResellerWithdrawal> findByPartnerAndKey(@Param("partnerId") UUID partnerId, @Param("key") String key);

    /** Verrou d'ecriture : deux actions simultanees sur le meme retrait se serialisent (la seconde voit le nouveau statut). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from ResellerWithdrawal w where w.id = :id")
    Optional<ResellerWithdrawal> lockById(@Param("id") UUID id);

    @Query("select w from ResellerWithdrawal w join fetch w.partner where w.id = :id")
    Optional<ResellerWithdrawal> findByIdWithPartner(@Param("id") UUID id);

    /** Retraits non terminés d'un partenaire : une seule demande en cours à la fois. */
    @Query("select count(w) from ResellerWithdrawal w where w.partner.id = :partnerId and w.status in :statuses")
    long countByPartnerAndStatuses(@Param("partnerId") UUID partnerId, @Param("statuses") Collection<PayoutStatus> statuses);
}

interface ResellerAuditLogRepository extends JpaRepository<ResellerAuditLog, UUID> {
    Page<ResellerAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

interface ResellerTermsAcceptanceRepository extends JpaRepository<ResellerTermsAcceptance, UUID> {
}

interface ResellerProductEligibilityRepository extends JpaRepository<ResellerProductEligibility, UUID> {
    @Query("select e.productId from ResellerProductEligibility e where e.excluded = true")
    List<UUID> findExcludedProductIds();
}
