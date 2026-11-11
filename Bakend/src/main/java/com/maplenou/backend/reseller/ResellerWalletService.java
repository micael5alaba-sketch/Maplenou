package com.maplenou.backend.reseller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.reseller.dto.ResellerDtos.*;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registre financier du partenaire. Les soldes ne sont JAMAIS stockés : ils se calculent par somme
 * des écritures immuables. Chaque écriture porte une clé d'idempotence ; rejouer un événement
 * (livraison validée deux fois, double clic) ne crédite donc jamais deux fois. Les opérations d'un
 * même partenaire sont sérialisées par un verrou sur son profil.
 */
@Service
@RequiredArgsConstructor
public class ResellerWalletService {

    private final ResellerWalletTransactionRepository txRepository;
    private final ResellerProfileRepository profileRepository;
    private final ResellerCommissionRepository commissionRepository;
    private final ResellerClickRepository clickRepository;
    private final ResellerAuditLogRepository auditRepository;
    private final AuditService auditService;

    /**
     * Ajoute une écriture si sa clé n'existe pas encore. Renvoie vrai si elle a été écrite.
     * Doit être appelé dans une transaction ; verrouille le profil pour sérialiser les soldes.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    boolean append(UUID partnerId, WalletTxType type, long amount, long availableDelta, long reservedDelta,
                   long fee, UUID referenceId, String reference, String reason, UUID actorId, String key) {
        profileRepository.lockById(partnerId);
        if (txRepository.existsByIdempotencyKey(key)) {
            return false;
        }
        txRepository.save(ResellerWalletTransaction.builder()
                .partnerId(partnerId).type(type).amount(amount)
                .availableDelta(availableDelta).reservedDelta(reservedDelta).feeAmount(fee)
                .referenceId(referenceId).reference(reference).reason(reason).actorId(actorId)
                .idempotencyKey(key).build());
        return true;
    }

    /** Solde disponible calculé (après verrou, pour une décision de retrait fiable). */
    @Transactional(propagation = Propagation.MANDATORY)
    long lockedAvailable(UUID partnerId) {
        profileRepository.lockById(partnerId);
        return txRepository.sumAvailable(partnerId);
    }

    @Transactional(readOnly = true)
    public WalletResponse wallet(UUID partnerId) {
        return new WalletResponse(
                txRepository.sumAvailable(partnerId),
                txRepository.sumReserved(partnerId),
                commissionRepository.sumShareByPartnerAndStatus(partnerId, ResellerCommissionStatus.PENDING),
                txRepository.sumAmountByType(partnerId, WalletTxType.COMMISSION_CREDIT),
                txRepository.sumAmountByType(partnerId, WalletTxType.WITHDRAWAL_PAID),
                txRepository.sumFees(partnerId),
                clickRepository.countByPartnerIdAndCountedTrue(partnerId));
    }

    @Transactional(readOnly = true)
    public Page<WalletTransactionResponse> transactions(UUID partnerId, Pageable pageable) {
        return txRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId, pageable)
                .map(t -> new WalletTransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getAvailableDelta(),
                        t.getReservedDelta(), t.getFeeAmount(), t.getReference(), t.getReason(), t.getCreatedAt()));
    }

    /**
     * Régularisation administrative (correction d'erreur, retour tardif...) : écriture signée avec
     * motif et auteur, jamais une modification d'une écriture existante. La clé fournie par le
     * client évite un double envoi.
     */
    @Transactional
    public WalletResponse adjust(UUID partnerId, AdjustmentRequest r, User actor, String ip) {
        profileRepository.findById(partnerId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
        if (r.amount() == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Le montant de la régularisation ne peut pas être nul");
        }
        boolean written = append(partnerId, WalletTxType.ADJUSTMENT, r.amount(), r.amount(), 0, 0, null, null,
                r.reason().trim(), actor.getId(), "adjustment:" + partnerId + ":" + r.requestKey());
        if (written) {
            auditRepository.save(ResellerAuditLog.builder()
                    .actorId(actor.getId()).action("WALLET_ADJUSTED").entityType("RESELLER_WALLET").entityId(partnerId)
                    .newValue(String.valueOf(r.amount())).reason(r.reason().trim()).build());
            auditService.log(actor.getId(), AuditAction.RESELLER_WALLET_ADJUSTED, "RESELLER_WALLET", partnerId,
                    r.amount() + " FCFA | " + r.reason().trim(), ip);
        }
        return wallet(partnerId);
    }
}
