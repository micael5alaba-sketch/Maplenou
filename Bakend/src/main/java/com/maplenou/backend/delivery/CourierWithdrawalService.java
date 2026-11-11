package com.maplenou.backend.delivery;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.delivery.dto.CourierWithdrawalResponse;
import com.maplenou.backend.delivery.dto.RequestCourierWithdrawalRequest;
import com.maplenou.backend.payout.PayoutStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Retraits des gains livreurs : même circuit obligatoire que les retraits vendeurs (voir
 * {@link WithdrawalWorkflow}). Remplace l'ancien marquage direct "gain payé" par un admin, qui
 * contournait toute vérification et toute approbation.
 */
@Service
@RequiredArgsConstructor
public class CourierWithdrawalService {

    private final CourierWithdrawalRepository withdrawalRepository;
    private final CourierEarningRepository earningRepository;
    private final WithdrawalWorkflow workflow;
    private final AuditService auditService;

    // ----- Livreur -----

    /** Demande de retrait de tous les gains disponibles (PENDING, pas déjà dans un retrait). */
    @Transactional
    public CourierWithdrawalResponse request(User agent, RequestCourierWithdrawalRequest request) {
        List<CourierEarning> available = earningRepository.findAvailableForWithdrawal(agent.getId());
        if (available.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aucun gain disponible pour un retrait pour le moment");
        }
        BigDecimal total = available.stream().map(CourierEarning::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        CourierWithdrawal withdrawal = withdrawalRepository.save(CourierWithdrawal.builder()
                .agent(agent)
                .amount(total)
                .paymentDetails(request.paymentDetails().trim())
                .status(PayoutStatus.PENDING)
                .build());
        for (CourierEarning earning : available) {
            earning.setWithdrawal(withdrawal);
            earningRepository.save(earning);
        }
        workflow.recordCreated(WithdrawalSubjectType.COURIER_WITHDRAWAL, withdrawal.getId(), agent,
                "Demande du livreur : " + total + " FCFA (" + available.size() + " gain(s))");
        auditService.log(agent.getId(), AuditAction.WITHDRAWAL_REQUESTED, "COURIER_WITHDRAWAL", withdrawal.getId(),
                "Retrait livreur demandé : " + total + " FCFA", null);
        return CourierWithdrawalResponse.from(withdrawal);
    }

    @Transactional(readOnly = true)
    public Page<CourierWithdrawalResponse> listMine(User agent, Pageable pageable) {
        return withdrawalRepository.findByAgent(agent.getId(), pageable).map(CourierWithdrawalResponse::from);
    }

    // ----- Personnel -----

    @Transactional(readOnly = true)
    public Page<CourierWithdrawalResponse> listAll(PayoutStatus status, Pageable pageable) {
        return withdrawalRepository.findByOptionalStatus(status, pageable).map(CourierWithdrawalResponse::from);
    }

    @Transactional(readOnly = true)
    public CourierWithdrawalResponse getById(UUID id) {
        return CourierWithdrawalResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<WithdrawalHistoryResponse> history(UUID id) {
        getOrThrow(id);
        return workflow.history(WithdrawalSubjectType.COURIER_WITHDRAWAL, id);
    }

    @Transactional
    public CourierWithdrawalResponse transition(UUID id, WithdrawalAction action, WithdrawalActionRequest request,
                                                User actor, String ip) {
        CourierWithdrawal withdrawal = getOrThrow(id);
        PayoutStatus to = workflow.apply(
                WithdrawalSubjectType.COURIER_WITHDRAWAL, id,
                "Livreur : " + withdrawal.getAgent().getFullName() + " | Montant : " + withdrawal.getAmount() + " FCFA",
                withdrawal.getStatus(), withdrawal.getApprovedAt() != null, action, actor,
                request != null ? request.notes() : null, request != null ? request.mfaCode() : null, ip);

        withdrawal.setStatus(to);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            String line = "[" + action + " " + Instant.now() + "] " + request.notes();
            withdrawal.setNotes(withdrawal.getNotes() == null || withdrawal.getNotes().isBlank()
                    ? line : withdrawal.getNotes() + "\n" + line);
        }
        switch (action) {
            case APPROVE -> {
                withdrawal.setApprovedBy(actor.getId());
                withdrawal.setApprovedAt(Instant.now());
            }
            case PAY -> {
                // Retrait approuvé puis payé : les gains inclus deviennent versés.
                Instant now = Instant.now();
                withdrawal.setPaidAt(now);
                for (CourierEarning earning : earningRepository.findByWithdrawalId(id)) {
                    earning.setStatus(CourierEarningStatus.PAID);
                    earning.setPaidAt(now);
                    earningRepository.save(earning);
                }
            }
            case REJECT -> {
                // Rejet : les gains redeviennent disponibles pour une nouvelle demande.
                for (CourierEarning earning : earningRepository.findByWithdrawalId(id)) {
                    earning.setWithdrawal(null);
                    earningRepository.save(earning);
                }
            }
            default -> { }
        }
        return CourierWithdrawalResponse.from(withdrawalRepository.save(withdrawal));
    }

    private CourierWithdrawal getOrThrow(UUID id) {
        return withdrawalRepository.findByIdWithAgent(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Retrait introuvable"));
    }
}
