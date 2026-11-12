package com.maplenou.backend.reseller.dto;

import com.maplenou.backend.reseller.ResellerCommissionStatus;
import com.maplenou.backend.reseller.ResellerPaymentMethod;
import com.maplenou.backend.reseller.ResellerStatus;
import com.maplenou.backend.reseller.WalletTxType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** DTOs du programme partenaire revendeur (records, jamais d'entité exposée). Montants en FCFA entiers. */
public final class ResellerDtos {

    private ResellerDtos() {
    }

    // ----- Demandes -----

    public record ApplyRequest(
            @AssertTrue(message = "Vous devez accepter les conditions du programme") boolean acceptTerms) {
    }

    public record ReasonRequest(String reason) {
    }

    public record ProfileResponse(UUID id, UUID userId, String fullName, String phoneNumber, String email,
                                  ResellerStatus status, String termsVersion, Instant termsAcceptedAt,
                                  Instant decidedAt, String statusReason, Instant createdAt) {
    }

    // ----- Paramètres -----

    public record SettingsResponse(int version, BigDecimal shareRate, long minUnitPrice, long tierThreshold,
                                   BigDecimal rateLow, BigDecimal rateHigh, int attributionDays,
                                   long minWithdrawal, BigDecimal withdrawalFeeRate, boolean programActive,
                                   String changeReason, Instant createdAt) {
    }

    public record UpdateSettingsRequest(
            @NotNull @DecimalMin("0.01") @DecimalMax("0.50") BigDecimal shareRate,
            @Min(1) long minUnitPrice,
            @Min(1) long tierThreshold,
            @NotNull @DecimalMin("0.001") @DecimalMax("0.50") BigDecimal rateLow,
            @NotNull @DecimalMin("0.001") @DecimalMax("0.50") BigDecimal rateHigh,
            @Min(1) @Max(365) int attributionDays,
            @Min(0) long minWithdrawal,
            @NotNull @DecimalMin("0") @DecimalMax("0.30") BigDecimal withdrawalFeeRate,
            boolean programActive,
            @NotBlank(message = "Un motif de modification est obligatoire") String reason) {
    }

    // ----- Catalogue et liens -----

    public record CatalogItem(UUID productId, String name, String shopName, String imageUrl, long unitPrice,
                              long partnerShare, String linkCode, String linkUrl, long clicks, long unitsSold) {
    }

    public record CreateLinkRequest(@NotNull UUID productId) {
    }

    public record LinkResponse(UUID id, UUID productId, String productName, String code, String url,
                               Instant createdAt) {
    }

    public record ClickRequest(@NotBlank @Size(max = 16) String code, @Size(max = 100) String visitorId) {
    }

    public record ClickResponse(boolean valid, UUID productId, String productSlug, String token, Instant expiresAt) {
    }

    // ----- Commissions et portefeuille -----

    public record CommissionResponse(UUID id, UUID orderItemId, UUID partnerId, String productName, int unitIndex,
                                     long unitPrice, BigDecimal commissionRate, long maplenouCommission,
                                     long partnerShare, long maplenouShare, int settingsVersion,
                                     ResellerCommissionStatus status, Instant createdAt, Instant creditedAt) {
    }

    /**
     * Vue PARTENAIRE d'une commission : prix retenu, commission Maplenou, part de 25 %, statut, date de credit.
     * Jamais le taux (5 % / 3 %) ni de donnees client (guide partenaire 10.3).
     */
    public record PartnerCommissionResponse(UUID id, String productName, int unitIndex, long unitPrice,
                                            long maplenouCommission, long partnerShare, ResellerCommissionStatus status,
                                            Instant createdAt, Instant creditedAt) {
    }

    /** Conditions visibles par le partenaire : sans les taux de commission Maplenou. */
    public record PartnerSettingsResponse(BigDecimal shareRate, long minUnitPrice, int attributionDays,
                                          long minWithdrawal, BigDecimal withdrawalFeeRate, boolean programActive) {
    }

    /** Statistiques sur une periode : clics comptes, ventes attribuees, unites vendues, taux de conversion (%). */
    public record StatsResponse(Instant from, Instant to, long clicks, long attributedSales, long unitsSold,
                                BigDecimal conversionRate) {
    }

    public record WalletResponse(long available, long reserved, long pending, long totalEarned, long totalPaid,
                                 long totalFees, long clicks) {
    }

    public record WalletTransactionResponse(UUID id, WalletTxType type, long amount, long availableDelta,
                                            long reservedDelta, long feeAmount, String reference, String reason,
                                            Instant createdAt) {
    }

    public record AdjustmentRequest(@NotNull Long amount, @NotBlank(message = "Un motif est obligatoire") String reason,
                                    @NotBlank @Size(max = 100) String requestKey) {
    }

    // ----- Retraits -----

    public record WithdrawRequest(@Min(1) long amount, @NotNull ResellerPaymentMethod method,
                                  @NotBlank @Pattern(regexp = "^\\+?[0-9 ]{8,16}$", message = "Numéro de paiement invalide")
                                  String paymentPhone) {
    }

    public record WithdrawalResponse(UUID id, UUID partnerId, String partnerName, long amount, long feeAmount,
                                     long netAmount, ResellerPaymentMethod method, String paymentPhone,
                                     com.maplenou.backend.payout.PayoutStatus status, String paymentReference,
                                     UUID approvedBy, Instant approvedAt, Instant paidAt, String notes, Instant createdAt) {
    }

    // ----- Statistiques admin -----

    public record AdminStats(long pendingRequests, long activePartners, long pendingCommissionsAmount,
                             long creditedCommissionsAmount, long totalClicksCounted) {
    }

    public record AuditEntry(UUID id, UUID actorId, String action, String entityType, UUID entityId,
                             String oldValue, String newValue, String reason, Instant createdAt) {
    }
}
