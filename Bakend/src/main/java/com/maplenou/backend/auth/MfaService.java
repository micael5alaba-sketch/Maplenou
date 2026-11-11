package com.maplenou.backend.auth;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.common.Sha256Utils;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Vérification MFA unifiée : code TOTP de l'application d'authentification, OU un code de
 * récupération à usage unique remis à l'activation (perte du téléphone). Utilisée à la connexion
 * (étape MFA) et à chaque approbation de retrait (nouvelle vérification exigée même session ouverte).
 */
@Service
@RequiredArgsConstructor
public class MfaService {

    static final int RECOVERY_CODE_COUNT = 10;
    static final int MAX_FAILURES = 5;
    static final int LOCK_MINUTES = 15;
    // Alphabet sans caractères ambigus (0/O, 1/I) : les codes sont recopiés à la main.
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final TotpService totpService;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final AuditService auditService;
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

    /**
     * Vrai si [code] est le code TOTP courant OU un code de récupération non utilisé (consommé).
     * Anti-devinette : après {@value #MAX_FAILURES} échecs consécutifs, toute vérification MFA de
     * ce compte est refusée pendant {@value #LOCK_MINUTES} minutes (connexion ET approbation de
     * retrait), même avec le bon code — un code à 6 chiffres ne doit pas pouvoir être deviné.
     */
    @Transactional
    public boolean verify(User user, String code) {
        String lockKey = "mfa_lock:" + user.getId();
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de codes MFA invalides. Réessayez dans " + LOCK_MINUTES + " minutes.", LOCK_MINUTES * 60);
        }

        boolean valid = false;
        if (user.getTotpSecret() != null && code != null && !code.isBlank()) {
            String trimmed = code.trim();
            valid = trimmed.matches("\\d{6}")
                    ? totpService.verifyCode(user.getTotpSecret(), trimmed)
                    : consumeRecoveryCode(user, trimmed);
        }

        String failKey = "mfa_fail:" + user.getId();
        if (valid) {
            redisTemplate.delete(failKey);
            return true;
        }
        Long failures = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, Duration.ofMinutes(LOCK_MINUTES));
        if (failures != null && failures >= MAX_FAILURES) {
            redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(LOCK_MINUTES));
            redisTemplate.delete(failKey);
        }
        return false;
    }

    /** Génère 10 nouveaux codes (remplace les précédents). Renvoyés UNE seule fois en clair. */
    @Transactional
    public List<String> generateRecoveryCodes(User user) {
        recoveryCodeRepository.deleteAllByUserId(user.getId());
        recoveryCodeRepository.flush();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = randomCode();
            codes.add(code);
            recoveryCodeRepository.save(MfaRecoveryCode.builder()
                    .userId(user.getId())
                    .codeHash(hash(code))
                    .build());
        }
        auditService.log(user.getId(), AuditAction.MFA_RECOVERY_CODES_GENERATED,
                "USER", user.getId(), RECOVERY_CODE_COUNT + " codes de récupération générés", null);
        return codes;
    }

    @Transactional
    public void deleteRecoveryCodes(User user) {
        recoveryCodeRepository.deleteAllByUserId(user.getId());
    }

    private boolean consumeRecoveryCode(User user, String code) {
        return recoveryCodeRepository.findByUserIdAndCodeHashAndUsedAtIsNull(user.getId(), hash(code))
                .map(c -> {
                    c.setUsedAt(Instant.now());
                    recoveryCodeRepository.save(c);
                    auditService.log(user.getId(), AuditAction.MFA_RECOVERY_CODE_USED,
                            "USER", user.getId(), "Code de récupération utilisé", null);
                    return true;
                })
                .orElse(false);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            if (i == 5) sb.append('-');
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // Normalise : majuscules, sans espaces autour (le tiret est conservé dans le format stocké)
    private String hash(String code) {
        return Sha256Utils.hex(code.trim().toUpperCase());
    }
}
