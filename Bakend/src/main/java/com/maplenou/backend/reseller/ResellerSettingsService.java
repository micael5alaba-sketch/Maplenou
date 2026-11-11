package com.maplenou.backend.reseller;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.reseller.dto.ResellerDtos.SettingsResponse;
import com.maplenou.backend.reseller.dto.ResellerDtos.UpdateSettingsRequest;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paramètres financiers versionnés : chaque modification ajoute une version (l'ancienne reste
 * lisible pour les commandes déjà créées). Seul le Super Admin (permission RESELLERS_SETTINGS) peut
 * modifier, avec un motif obligatoire, tracé dans les deux journaux d'audit.
 */
@Service
@RequiredArgsConstructor
public class ResellerSettingsService {

    private final ResellerSettingsRepository repository;
    private final ResellerAuditLogRepository resellerAuditRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    ResellerSettings current() {
        return repository.findFirstByOrderByVersionDesc()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Paramètres du programme partenaire introuvables"));
    }

    @Transactional(readOnly = true)
    public SettingsResponse getCurrent() {
        return toResponse(current());
    }

    @Transactional(readOnly = true)
    public Page<SettingsResponse> history(Pageable pageable) {
        return repository.findAllByOrderByVersionDesc(pageable).map(ResellerSettingsService::toResponse);
    }

    @Transactional
    public SettingsResponse update(UpdateSettingsRequest r, User actor, String ip) {
        if (r.rateLow().compareTo(r.rateHigh()) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le taux des prix bas ne peut pas être inférieur au taux des prix élevés");
        }
        if (r.tierThreshold() < r.minUnitPrice()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Le seuil des paliers doit être supérieur ou égal au prix minimum");
        }
        ResellerSettings old = current();
        ResellerSettings next = repository.save(ResellerSettings.builder()
                .version(old.getVersion() + 1)
                .shareRate(r.shareRate())
                .minUnitPrice(r.minUnitPrice())
                .tierThreshold(r.tierThreshold())
                .rateLow(r.rateLow())
                .rateHigh(r.rateHigh())
                .attributionDays(r.attributionDays())
                .minWithdrawal(r.minWithdrawal())
                .withdrawalFeeRate(r.withdrawalFeeRate())
                .programActive(r.programActive())
                .changeReason(r.reason().trim())
                .createdBy(actor.getId())
                .build());
        resellerAuditRepository.save(ResellerAuditLog.builder()
                .actorId(actor.getId()).action("SETTINGS_CHANGED").entityType("RESELLER_SETTINGS")
                .entityId(next.getId()).oldValue(describe(old)).newValue(describe(next))
                .reason(r.reason().trim()).build());
        auditService.log(actor.getId(), AuditAction.RESELLER_SETTINGS_CHANGED, "RESELLER_SETTINGS", next.getId(),
                "v" + old.getVersion() + " → v" + next.getVersion() + " | " + r.reason().trim(), ip);
        return toResponse(next);
    }

    static String describe(ResellerSettings s) {
        return "v" + s.getVersion() + " part=" + s.getShareRate() + " min=" + s.getMinUnitPrice()
                + " seuil=" + s.getTierThreshold() + " taux=" + s.getRateLow() + "/" + s.getRateHigh()
                + " jours=" + s.getAttributionDays() + " retraitMin=" + s.getMinWithdrawal()
                + " frais=" + s.getWithdrawalFeeRate() + " actif=" + s.isProgramActive();
    }

    static SettingsResponse toResponse(ResellerSettings s) {
        return new SettingsResponse(s.getVersion(), s.getShareRate(), s.getMinUnitPrice(), s.getTierThreshold(),
                s.getRateLow(), s.getRateHigh(), s.getAttributionDays(), s.getMinWithdrawal(),
                s.getWithdrawalFeeRate(), s.isProgramActive(), s.getChangeReason(), s.getCreatedAt());
    }
}
