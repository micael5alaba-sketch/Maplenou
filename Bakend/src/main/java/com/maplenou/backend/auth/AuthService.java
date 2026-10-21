package com.maplenou.backend.auth;

import com.maplenou.backend.audit.AuditAction;
import com.maplenou.backend.audit.AuditService;
import com.maplenou.backend.auth.dto.AuthResponse;
import com.maplenou.backend.auth.dto.LoginRequest;
import com.maplenou.backend.auth.dto.LoginResponse;
import com.maplenou.backend.auth.dto.LogoutRequest;
import com.maplenou.backend.auth.dto.MfaLoginRequest;
import com.maplenou.backend.auth.dto.RefreshRequest;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.auth.dto.ResetPasswordRequest;
import com.maplenou.backend.common.ClientIpResolver;
import com.maplenou.backend.common.Sha256Utils;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.email.EmailService;
import com.maplenou.backend.security.JwtService;
import com.maplenou.backend.security.RefreshTokenSessionService;
import com.maplenou.backend.security.TokenBlacklistService;
import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS_PER_IP = 5;
    private static final int LOCKOUT_MINUTES_PER_IP = 15;
    // Verrou "identifiant seul" (ignore l'IP) : ferme le trou signalé par le pentest du
    // 21/10/2026 — un attaquant distribuant ses tentatives sur de nombreuses IPs ne dépassait
    // jamais 4 échecs par IP, donc ne déclenchait jamais le verrou ci-dessus (19 échecs cumulés
    // acceptés sans 429 ni verrouillage). Pas de délai progressif/CAPTCHA pour l'instant (aucune
    // infra de challenge côté client) — seuil dur uniquement, à durcir plus tard si besoin.
    private static final int MAX_FAILED_ATTEMPTS_GLOBAL = 10;
    private static final int LOCKOUT_MINUTES_GLOBAL = 30;

    private static final int PASSWORD_RESET_OTP_TTL_MINUTES = 10;
    private static final int MAX_RESET_CODE_ATTEMPTS = 5;
    // F-01 : limites dédiées à forgot-password, distinctes du rate limit global /api/auth/**
    // (10/min, trop large pour empêcher l'énumération ou l'automatisation sur cette route précise).
    private static final int MAX_FORGOT_PASSWORD_PER_NUMBER = 5;
    private static final int MAX_FORGOT_PASSWORD_PER_IP = 30;
    private static final int FORGOT_PASSWORD_WINDOW_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenSessionService refreshTokenSessionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final AuditService auditService;
    private final ClientIpResolver clientIpResolver;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
        }

        // role = null par defaut (compte acheteur standard). Seul "DELIVERY_AGENT" peut etre
        // demande explicitement a l'inscription (livreur independant, cahier des charges §3.3) :
        // toute autre valeur (y compris "ADMIN", qui ne doit JAMAIS etre auto-attribuable) est
        // rejetee ici, independamment de ce que l'enum Role accepterait par ailleurs.
        Role role = null;
        if (request.accountType() != null && !request.accountType().isBlank()) {
            if (!"DELIVERY_AGENT".equals(request.accountType())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Type de compte invalide");
            }
            role = Role.DELIVERY_AGENT;
        }

        User user = User.builder()
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .active(true)
                .phoneVerified(false)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user, ipHash(httpRequest));
    }

    /**
     * Double verrou (F-03, pentest du 21/10/2026) :
     *   - par (téléphone, IP) : 5 échecs / 15 min -> verrou 15 min (comportement pré-existant,
     *     protège l'utilisateur légitime connecté depuis une IP différente de l'attaquant).
     *   - par téléphone seul, toutes IP confondues : 10 échecs / 30 min -> verrou 30 min (nouveau
     *     — seul rempart contre un attaquant distribué sur de nombreuses IPs différentes).
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIpResolver.resolve(httpRequest);
        String phone = request.phoneNumber();

        String ipLockKey = "login_lock:" + phone + ":" + ip;
        String ipFailKey = "login_fail:" + phone + ":" + ip;
        String globalLockKey = "login_lock_global:" + phone;
        String globalFailKey = "login_fail_global:" + phone;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(ipLockKey))) {
            throw lockedResponse(LOCKOUT_MINUTES_PER_IP);
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(globalLockKey))) {
            throw lockedResponse(LOCKOUT_MINUTES_GLOBAL);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(phone, request.password())
            );
        } catch (BadCredentialsException e) {
            registerFailure(ipFailKey, ipLockKey, MAX_FAILED_ATTEMPTS_PER_IP, LOCKOUT_MINUTES_PER_IP);
            registerFailure(globalFailKey, globalLockKey, MAX_FAILED_ATTEMPTS_GLOBAL, LOCKOUT_MINUTES_GLOBAL);
            log.warn("AUTH_LOGIN_FAILED phoneHash={} ipHash={}", Sha256Utils.hex(phone), Sha256Utils.hex(ip));
            throw e;
        }

        // Réinitialise les deux compteurs d'échecs après un succès
        redisTemplate.delete(ipFailKey);
        redisTemplate.delete(globalFailKey);

        User user = userRepository.findByPhoneNumber(phone)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (user.isTotpEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getPhoneNumber());
            return LoginResponse.mfaChallenge(mfaToken);
        }

        AuthResponse auth = buildAuthResponse(user, ipHash(httpRequest));
        return LoginResponse.fullAuth(auth.accessToken(), auth.refreshToken(), auth.user());
    }

    private void registerFailure(String failKey, String lockKey, int maxAttempts, int lockoutMinutes) {
        Long attempts = redisTemplate.opsForValue().increment(failKey);
        redisTemplate.expire(failKey, Duration.ofMinutes(lockoutMinutes));
        if (attempts != null && attempts >= maxAttempts) {
            redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(lockoutMinutes));
            redisTemplate.delete(failKey);
        }
    }

    private ApiException lockedResponse(int lockoutMinutes) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Compte temporairement verrouillé suite à de trop nombreuses tentatives. Réessayez dans "
                        + lockoutMinutes + " minutes.",
                lockoutMinutes * 60);
    }

    public AuthResponse verifyMfaLogin(MfaLoginRequest request, HttpServletRequest httpRequest) {
        String mfaToken = request.mfaToken();

        if (!jwtService.isMfaToken(mfaToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token MFA invalide");
        }

        if (jwtService.isTokenExpired(mfaToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token MFA expiré, veuillez vous reconnecter");
        }

        // Rejeter un token MFA déjà utilisé (anti-replay)
        if (tokenBlacklistService.isBlacklisted(mfaToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token MFA déjà utilisé");
        }

        String phoneNumber = jwtService.extractUsername(mfaToken);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        if (!user.isTotpEnabled()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La 2FA n'est pas activée pour ce compte");
        }

        if (!totpService.verifyCode(user.getTotpSecret(), request.code())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Code TOTP invalide ou expiré");
        }

        // Invalider le token MFA pour éviter la réutilisation
        Date mfaExpiry = jwtService.extractExpiration(mfaToken);
        tokenBlacklistService.blacklist(mfaToken, mfaExpiry.getTime());

        return buildAuthResponse(user, ipHash(httpRequest));
    }

    public AuthResponse refresh(RefreshRequest request, HttpServletRequest httpRequest) {
        String token = request.refreshToken();

        if (!jwtService.isRefreshToken(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement invalide");
        }

        if (tokenBlacklistService.isBlacklisted(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token révoqué, veuillez vous reconnecter");
        }

        String phoneNumber = jwtService.extractUsername(token);
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        if (!jwtService.isTokenValid(token, user)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de rafraîchissement expiré ou invalide");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // F-02 : rotation stricte à usage unique — lève une 401 et révoque toute la famille de
        // session si ce jeton a déjà été échangé auparavant (rejeu).
        refreshTokenSessionService.rotate(user, token, newRefreshToken, ipHash(httpRequest));

        return new AuthResponse(newAccessToken, newRefreshToken, UserResponse.from(user));
    }

    public void logout(LogoutRequest request) {
        // Révoquer le refresh token (blacklist immédiate) et toute sa famille de session (F-02 :
        // la déconnexion doit invalider la session entière, pas seulement le dernier jeton connu).
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token de rafraîchissement invalide");
        }
        Date refreshExpiry = jwtService.extractExpiration(refreshToken);
        tokenBlacklistService.blacklist(refreshToken, refreshExpiry.getTime());
        refreshTokenSessionService.revokeFamilyByToken(refreshToken, "LOGOUT");

        // Révoquer également l'access token si fourni (invalidation immédiate)
        if (request.accessToken() != null && !request.accessToken().isBlank()) {
            try {
                Date accessExpiry = jwtService.extractExpiration(request.accessToken());
                tokenBlacklistService.blacklist(request.accessToken(), accessExpiry.getTime());
            } catch (Exception ignored) {
                // Access token malformé ou déjà expiré — on ignore
            }
        }
    }

    /**
     * F-01 (pentest du 21/10/2026) : réponse strictement identique quel que soit le résultat réel
     * (numéro inconnu, compte sans email, compte éligible, échec d'envoi) — avant ce correctif,
     * cette route distinguait ces cas par son code HTTP (404/400/204), ce qui permettait de
     * déterminer quels numéros de téléphone possèdent un compte Maplenou. Le résultat réel n'est
     * tracé que côté serveur (log structuré), jamais renvoyé au client.
     */
    public void forgotPassword(String phoneNumber, HttpServletRequest httpRequest) {
        String ip = clientIpResolver.resolve(httpRequest);

        if (isOverRequestLimit("forgot_pwd_req:" + phoneNumber, MAX_FORGOT_PASSWORD_PER_NUMBER, FORGOT_PASSWORD_WINDOW_MINUTES)
                || isOverRequestLimit("forgot_pwd_req_ip:" + ip, MAX_FORGOT_PASSWORD_PER_IP, FORGOT_PASSWORD_WINDOW_MINUTES)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Trop de demandes. Réessayez plus tard.",
                    FORGOT_PASSWORD_WINDOW_MINUTES * 60);
        }

        String outcome;
        try {
            User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
            if (user == null) {
                outcome = "UNKNOWN";
            } else if (user.getEmail() == null || user.getEmail().isBlank()) {
                outcome = "INELIGIBLE";
            } else {
                String code = String.format("%06d", secureRandom.nextInt(1_000_000));
                redisTemplate.opsForValue().set("password_reset:" + phoneNumber, Sha256Utils.hex(code),
                        Duration.ofMinutes(PASSWORD_RESET_OTP_TTL_MINUTES));
                redisTemplate.delete("password_reset_attempts:" + phoneNumber);
                emailService.sendPasswordResetCode(user.getEmail(), code);
                outcome = "QUEUED";
            }
        } catch (Exception e) {
            log.error("forgot-password: échec d'envoi (ipHash={}) : {}", Sha256Utils.hex(ip), e.getMessage());
            outcome = "PROVIDER_FAILED";
        }
        log.info("forgot-password outcome={} ipHash={}", outcome, Sha256Utils.hex(ip));
    }

    /** Fenêtre glissante simple (compteur + TTL posé une seule fois) — même principe que
     * RateLimitFilter, en Redis pur (pas besoin du script Lua ici, un seul process incrémente
     * cette clé précise par appel, la fenêtre de course sur le premier INCR est sans conséquence). */
    private boolean isOverRequestLimit(String key, int max, int windowMinutes) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(windowMinutes));
        }
        return count != null && count > max;
    }

    /**
     * O-02 (pentest du 21/10/2026) : le code est haché au repos (jamais stocké en clair dans
     * Redis), comparé en temps constant, et invalidé après {@value MAX_RESET_CODE_ATTEMPTS}
     * tentatives erronées (même s'il restait valide par ailleurs) — avant ce correctif, rien ne
     * limitait le nombre d'essais sur un code à 6 chiffres pendant sa fenêtre de validité.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String phoneNumber = request.phoneNumber();
        String codeKey = "password_reset:" + phoneNumber;
        String attemptsKey = "password_reset_attempts:" + phoneNumber;

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(PASSWORD_RESET_OTP_TTL_MINUTES));
        }
        if (attempts != null && attempts > MAX_RESET_CODE_ATTEMPTS) {
            redisTemplate.delete(codeKey);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Demandez un nouveau code.", PASSWORD_RESET_OTP_TTL_MINUTES * 60);
        }

        String storedHash = redisTemplate.opsForValue().get(codeKey);
        if (!Sha256Utils.matches(storedHash, request.code())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code invalide ou expiré");
        }

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Aucun compte avec ce numéro de téléphone"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        redisTemplate.delete(codeKey);
        redisTemplate.delete(attemptsKey);

        // Une fuite de session (vol de refresh token) ne doit pas survivre à un changement de
        // mot de passe déclenché par la victime — révoque toutes les sessions actives.
        refreshTokenSessionService.revokeAllForUser(user.getId(), "PASSWORD_RESET");

        auditService.log(user.getId(), AuditAction.PASSWORD_CHANGED, "USER", user.getId(),
                "Mot de passe réinitialisé via code email", null);
    }

    private AuthResponse buildAuthResponse(User user, String ipHash) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenSessionService.createInitialSession(user, refreshToken, ipHash);
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    private String ipHash(HttpServletRequest httpRequest) {
        return Sha256Utils.hex(clientIpResolver.resolve(httpRequest));
    }
}
