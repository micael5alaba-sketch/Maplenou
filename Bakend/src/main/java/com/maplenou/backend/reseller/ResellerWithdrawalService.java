package com.maplenou.backend.reseller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.notification.NotificationService;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.reseller.dto.ResellerDtos.WithdrawRequest;
import com.maplenou.backend.reseller.dto.ResellerDtos.WithdrawalResponse;
import com.maplenou.backend.user.User;
import com.maplenou.backend.withdrawal.WithdrawalAction;
import com.maplenou.backend.withdrawal.WithdrawalSubjectType;
import com.maplenou.backend.withdrawal.WithdrawalWorkflow;
import com.maplenou.backend.withdrawal.dto.WithdrawalActionRequest;
import com.maplenou.backend.withdrawal.dto.WithdrawalHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Retraits des partenaires : même circuit obligatoire que les autres retraits (WithdrawalWorkflow :
 * préparation par l'Agent support, approbation du Super Admin avec MFA, historique, audit). Le
 * montant est réservé dès la demande ; les frais ne sont prélevés qu'au paiement effectif ; un rejet
 * libère la réserve.
 */
@Service
@RequiredArgsConstructor
public class ResellerWithdrawalService {

    private static final EnumSet<PayoutStatus> OPEN = EnumSet.of(PayoutStatus.PENDING, PayoutStatus.PREPARED,
            PayoutStatus.SUBMITTED, PayoutStatus.NEEDS_CORRECTION, PayoutStatus.APPROVED, PayoutStatus.PROCESSING);

    private final ResellerWithdrawalRepository withdrawalRepository;
    private final ResellerService resellerService;
    private final ResellerSettingsService settingsService;
    private final ResellerWalletService walletService;
    private final WithdrawalWorkflow workflow;
    private final AuditService auditService;
    private final NotificationService notificationService;

    // ----- Partenaire -----

    @Transactional
    public WithdrawalResponse request(User user, WithdrawRequest r, String idempotencyKey) {
        ResellerProfile partner = resellerService.requireActive(user);
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
        if (key != null && key.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cle d'idempotence trop longue");
        }
        ResellerSettings settings = settingsService.current();
        if (r.amount() < settings.getMinWithdrawal()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le montant minimum de retrait est de " + settings.getMinWithdrawal() + " FCFA");
        }
        long available = walletService.lockedAvailable(partner.getId());
        // Rejeu de la meme demande (verifie APRES le verrou du portefeuille : deux envois simultanes se
        // serialisent, le second retrouve le retrait du premier au lieu d'en creer un autre).
        if (key != null) {
            var replay = withdrawalRepository.findByPartnerAndKey(partner.getId(), key);
            if (replay.isPresent()) return toResponse(replay.get());
        }
        if (r.amount() > available) {
            throw new ApiException(HttpStatus.CONFLICT, "Solde disponible insuffisant (" + available + " FCFA)");
        }
        if (withdrawalRepository.countByPartnerAndStatuses(partner.getId(), OPEN) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Une demande de retrait est déjà en cours de traitement");
        }
        long fee = BigDecimal.valueOf(r.amount()).multiply(settings.getWithdrawalFeeRate())
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
        ResellerWithdrawal w = withdrawalRepository.save(ResellerWithdrawal.builder()
                .partner(partner).amount(r.amount()).feeAmount(fee).netAmount(r.amount() - fee)
                .method(r.method()).paymentPhone(r.paymentPhone().trim()).status(PayoutStatus.PENDING)
                .idempotencyKey(key).build());
        walletService.append(partner.getId(), WalletTxType.WITHDRAWAL_RESERVE, r.amount(), -r.amount(), r.amount(),
                0, w.getId(), null, "Demande de retrait", user.getId(), "wd-reserve:" + w.getId());
        workflow.recordCreated(WithdrawalSubjectType.PARTNER_WITHDRAWAL, w.getId(), user,
                "Demande du partenaire : " + r.amount() + " FCFA (" + r.method() + ")");
        auditService.log(user.getId(), AuditAction.WITHDRAWAL_REQUESTED, "PARTNER_WITHDRAWAL", w.getId(),
                "Retrait partenaire demandé : " + r.amount() + " FCFA", null);
        return toResponse(w);
    }

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> listMine(User user, Pageable pageable) {
        ResellerProfile partner = resellerService.requireProfile(user);
        return withdrawalRepository.findByPartner(partner.getId(), pageable).map(ResellerWithdrawalService::toResponse);
    }

    // ----- Personnel -----

    @Transactional(readOnly = true)
    public Page<WithdrawalResponse> listAll(PayoutStatus status, Pageable pageable) {
        return withdrawalRepository.findByOptionalStatus(status, pageable).map(ResellerWithdrawalService::toResponse);
    }

    @Transactional(readOnly = true)
    public WithdrawalResponse get(UUID id) {
        return toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public List<WithdrawalHistoryResponse> history(UUID id) {
        load(id);
        return workflow.history(WithdrawalSubjectType.PARTNER_WITHDRAWAL, id);
    }

    @Transactional
    public WithdrawalResponse transition(UUID id, WithdrawalAction action, WithdrawalActionRequest request,
                                         User actor, String ip) {
        withdrawalRepository.lockById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Retrait introuvable"));
        ResellerWithdrawal w = load(id);
        UUID partnerId = w.getPartner().getId();
        PayoutStatus to = workflow.apply(
                WithdrawalSubjectType.PARTNER_WITHDRAWAL, id,
                "Partenaire : " + w.getPartner().getFullName() + " | Montant : " + w.getAmount() + " FCFA",
                w.getStatus(), w.getApprovedAt() != null, action, actor,
                request != null ? request.notes() : null, request != null ? request.mfaCode() : null, ip);

        w.setStatus(to);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            String line = "[" + action + " " + Instant.now() + "] " + request.notes();
            w.setNotes(w.getNotes() == null || w.getNotes().isBlank() ? line : w.getNotes() + "\n" + line);
        }
        switch (action) {
            case APPROVE -> {
                w.setApprovedBy(actor.getId());
                w.setApprovedAt(Instant.now());
                notificationService.resellerEvent(w.getPartner().getUser().getId(), "Retrait approuvé",
                        "Votre demande de retrait de " + w.getAmount() + " FCFA a été approuvée.",
                        "RESELLER_WITHDRAWAL", w.getId());
            }
            case PAY -> {
                w.setPaidAt(Instant.now());
                if (request != null && request.notes() != null && !request.notes().isBlank()) {
                    w.setPaymentReference(request.notes().trim().length() > 100
                            ? request.notes().trim().substring(0, 100) : request.notes().trim());
                }
                // Les frais ne sont prélevés qu'ici, au paiement effectif.
                walletService.append(partnerId, WalletTxType.WITHDRAWAL_PAID, w.getAmount(), 0, -w.getAmount(),
                        w.getFeeAmount(), w.getId(), w.getPaymentReference(), "Retrait payé", actor.getId(),
                        "wd-paid:" + w.getId());
                notificationService.resellerEvent(w.getPartner().getUser().getId(), "Retrait payé",
                        "Votre retrait de " + w.getAmount() + " FCFA a été payé (net : " + w.getNetAmount() + " FCFA).",
                        "RESELLER_WITHDRAWAL", w.getId());
            }
            case REJECT -> {
                // Rejet : la réserve est libérée, le montant redevient disponible.
                walletService.append(partnerId, WalletTxType.WITHDRAWAL_RELEASE, w.getAmount(), w.getAmount(),
                        -w.getAmount(), 0, w.getId(), null, "Retrait rejeté", actor.getId(),
                        "wd-release:" + w.getId());
                notificationService.resellerEvent(w.getPartner().getUser().getId(), "Retrait rejeté",
                        "Votre demande de retrait a été rejetée. Le montant est de nouveau disponible.",
                        "RESELLER_WITHDRAWAL", w.getId());
            }
            default -> { }
        }
        return toResponse(withdrawalRepository.save(w));
    }

    private ResellerWithdrawal load(UUID id) {
        return withdrawalRepository.findByIdWithPartner(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Retrait introuvable"));
    }

    static WithdrawalResponse toResponse(ResellerWithdrawal w) {
        return new WithdrawalResponse(w.getId(), w.getPartner().getId(), w.getPartner().getFullName(), w.getAmount(),
                w.getFeeAmount(), w.getNetAmount(), w.getMethod(), w.getPaymentPhone(), w.getStatus(),
                w.getPaymentReference(), w.getApprovedBy(), w.getApprovedAt(), w.getPaidAt(), w.getNotes(), w.getCreatedAt());
    }
}
