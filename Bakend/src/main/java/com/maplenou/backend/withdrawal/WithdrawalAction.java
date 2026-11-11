package com.maplenou.backend.withdrawal;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.payout.PayoutStatus;
import com.maplenou.backend.security.Permission;

import java.util.EnumSet;
import java.util.Set;

import static com.maplenou.backend.payout.PayoutStatus.*;

/**
 * Actions du circuit des retraits et leurs règles : statuts de départ autorisés, statut d'arrivée,
 * permission requise, justification obligatoire, nouvelle vérification MFA. Centralisé ici pour
 * que la séparation préparation (Agent support) / autorisation financière (Super Admin) ne
 * dépende d'aucun contrôle dispersé.
 */
public enum WithdrawalAction {

    /** Vérification terminée : le dossier est conforme. */
    PREPARE(EnumSet.of(PENDING, NEEDS_CORRECTION), PREPARED, Permission.PAYOUTS_PREPARE, false, false, AuditAction.WITHDRAWAL_PREPARED),
    /** Renvoi pour correction (donnée ou justificatif manquant) : motif obligatoire. */
    RETURN_FOR_CORRECTION(EnumSet.of(PENDING, PREPARED), NEEDS_CORRECTION, Permission.PAYOUTS_PREPARE, true, false, AuditAction.WITHDRAWAL_RETURNED_FOR_CORRECTION),
    /** Transmission au Super Admin avec l'historique et les observations. */
    SUBMIT(EnumSet.of(PREPARED), SUBMITTED, Permission.PAYOUTS_PREPARE, false, false, AuditAction.WITHDRAWAL_SUBMITTED),
    /** Approbation finale : Super Admin, MFA redemandée même session ouverte. */
    APPROVE(EnumSet.of(SUBMITTED), APPROVED, Permission.PAYOUTS_APPROVE, false, true, AuditAction.WITHDRAWAL_APPROVED),
    /** Rejet : justification obligatoire. */
    REJECT(EnumSet.of(SUBMITTED), REJECTED, Permission.PAYOUTS_APPROVE, true, false, AuditAction.WITHDRAWAL_REJECTED),
    /** Virement lancé (après approbation uniquement). */
    PROCESS(EnumSet.of(APPROVED, FAILED), PROCESSING, Permission.PAYOUTS_APPROVE, false, false, AuditAction.WITHDRAWAL_PREPARED),
    /** Virement constaté : seul un retrait approuvé peut être payé. */
    PAY(EnumSet.of(APPROVED, PROCESSING), PAID, Permission.PAYOUTS_APPROVE, false, false, AuditAction.WITHDRAWAL_MARKED_PAID),
    /** Échec du virement : motif obligatoire. */
    FAIL(EnumSet.of(APPROVED, PROCESSING), FAILED, Permission.PAYOUTS_APPROVE, true, false, AuditAction.WITHDRAWAL_MARKED_FAILED);

    private final Set<PayoutStatus> allowedFrom;
    private final PayoutStatus target;
    private final Permission requiredPermission;
    private final boolean justificationRequired;
    private final boolean mfaRequired;
    private final AuditAction auditAction;

    WithdrawalAction(Set<PayoutStatus> allowedFrom, PayoutStatus target, Permission requiredPermission,
                     boolean justificationRequired, boolean mfaRequired, AuditAction auditAction) {
        this.allowedFrom = allowedFrom;
        this.target = target;
        this.requiredPermission = requiredPermission;
        this.justificationRequired = justificationRequired;
        this.mfaRequired = mfaRequired;
        this.auditAction = auditAction;
    }

    public Set<PayoutStatus> allowedFrom() { return allowedFrom; }
    public PayoutStatus target() { return target; }
    public Permission requiredPermission() { return requiredPermission; }
    public boolean justificationRequired() { return justificationRequired; }
    public boolean mfaRequired() { return mfaRequired; }
    public AuditAction auditAction() { return auditAction; }
}
