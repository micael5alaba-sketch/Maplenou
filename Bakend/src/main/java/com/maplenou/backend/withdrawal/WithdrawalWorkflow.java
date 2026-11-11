package com.maplenou.backend.withdrawal;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.auth.MfaService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.user.User;
import com.maplenou.backend.withdrawal.dto.WithdrawalHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Circuit obligatoire des retraits (vendeurs ET livreurs), appliqué ici une seule fois :
 * statut de départ valide, permission de l'acteur (vérifiée de nouveau côté service, pas
 * seulement par la route), justification obligatoire, nouvelle vérification MFA à l'approbation,
 * historique de transition et trace dans le journal d'audit. Les services métier (PayoutService,
 * CourierWithdrawalService) délèguent ici la décision puis appliquent leurs effets propres.
 */
@Service
@RequiredArgsConstructor
public class WithdrawalWorkflow {

    private final MfaService mfaService;
    private final AuditService auditService;
    private final WithdrawalStatusHistoryRepository historyRepository;

    /**
     * Valide et enregistre une transition. Renvoie le nouveau statut ; l'appelant l'applique à
     * l'entité et gère ses effets (approbateur, date de paiement, libération des montants...).
     *
     * @param wasApproved vrai si le retrait a déjà été approuvé (permet de relancer un virement FAILED)
     */
    @Transactional
    public PayoutStatus apply(WithdrawalSubjectType type, UUID subjectId, String label,
                              PayoutStatus current, boolean wasApproved,
                              WithdrawalAction action, User actor,
                              String note, String mfaCode, String ip) {

        if (!actor.permissions().contains(action.requiredPermission())) {
            auditService.log(actor.getId(), AuditAction.ACCESS_DENIED, type.name(), subjectId,
                    "Action " + action + " refusée : permission " + action.requiredPermission() + " manquante", ip);
            throw new AccessDeniedException("Permission manquante pour " + action);
        }

        // Un virement FAILED ne peut être relancé que s'il avait bien été approuvé auparavant.
        boolean invalidTransition = !action.allowedFrom().contains(current)
                || (action == WithdrawalAction.PROCESS && current == PayoutStatus.FAILED && !wasApproved);
        if (invalidTransition) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Action impossible : un retrait au statut " + current + " ne peut pas faire l'objet de « " + action + " »");
        }

        if (action.justificationRequired() && (note == null || note.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Une justification est obligatoire pour cette action");
        }

        if (action.mfaRequired()) {
            verifyMfa(type, subjectId, actor, mfaCode, ip);
        }

        PayoutStatus target = action.target();
        historyRepository.save(WithdrawalStatusHistory.builder()
                .subjectType(type)
                .subjectId(subjectId)
                .fromStatus(current)
                .toStatus(target)
                .actorId(actor.getId())
                .note(note)
                .build());
        auditService.log(actor.getId(), auditActionFor(action), type.name(), subjectId,
                label + " | " + current + " → " + target + (note != null && !note.isBlank() ? " | " + note : ""), ip);
        return target;
    }

    /** Enregistre la création d'un retrait (première ligne d'historique, statut PENDING). */
    @Transactional
    public void recordCreated(WithdrawalSubjectType type, UUID subjectId, User actor, String label) {
        historyRepository.save(WithdrawalStatusHistory.builder()
                .subjectType(type)
                .subjectId(subjectId)
                .fromStatus(null)
                .toStatus(PayoutStatus.PENDING)
                .actorId(actor != null ? actor.getId() : null)
                .note(label)
                .build());
    }

    @Transactional(readOnly = true)
    public List<WithdrawalHistoryResponse> history(WithdrawalSubjectType type, UUID subjectId) {
        return historyRepository.findBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(type, subjectId).stream()
                .map(WithdrawalHistoryResponse::from)
                .toList();
    }

    // ----- Privé -----

    /** MFA obligatoire à l'approbation, même si la session est déjà ouverte. */
    private void verifyMfa(WithdrawalSubjectType type, UUID subjectId, User actor, String mfaCode, String ip) {
        if (!actor.isTotpEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Activez la MFA sur votre compte avant d'approuver un retrait");
        }
        if (mfaCode == null || mfaCode.isBlank() || !mfaService.verify(actor, mfaCode)) {
            auditService.log(actor.getId(), AuditAction.WITHDRAWAL_MFA_FAILED, type.name(), subjectId,
                    "Code MFA invalide lors d'une approbation", ip);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Code MFA invalide ou expiré");
        }
    }

    private AuditAction auditActionFor(WithdrawalAction action) {
        return action == WithdrawalAction.PROCESS ? AuditAction.WITHDRAWAL_PROCESSING : action.auditAction();
    }
}
