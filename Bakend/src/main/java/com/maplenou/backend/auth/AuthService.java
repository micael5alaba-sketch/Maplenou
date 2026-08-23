package com.maplenou.backend.auth;

import com.maplenou.backend.auth.dto.AuthResponse;
import com.maplenou.backend.auth.dto.LoginRequest;
import com.maplenou.backend.auth.dto.LoginResponse;
import com.maplenou.backend.auth.dto.LogoutRequest;
import com.maplenou.backend.auth.dto.MfaLoginRequest;
import com.maplenou.backend.auth.dto.RefreshRequest;
import com.maplenou.backend.auth.dto.RegisterRequest;
import com.maplenou.backend.common.exception.ApiException;
import com.maplenou.backend.security.JwtService;
import com.maplenou.backend.security.TokenBlacklistService;
import com.maplenou.backend.user.User;
import com.maplenou.backend.user.UserRepository;
import com.maplenou.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.util.Date;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà utilisé");
        }

        // role = null : compte acheteur standard. ADMIN/DELIVERY_AGENT sont attribues
        // separement (creation interne), jamais via cette inscription publique.
        User user = User.builder()
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(null)
                .active(true)
                .phoneVerified(false)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        String lockKey = "login_lock:" + request.phoneNumber();
        String failKey = "login_fail:" + request.phoneNumber();

        // Vérifie si le compte est verrouillé
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Compte temporairement verrouillé suite à de trop nombreuses tentatives. Réessayez dans " + LOCKOUT_MINUTES + " minutes.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.phoneNumber(), request.password())
            );
        } catch (BadCredentialsException e) {
            // Incrémente le compteur d'échecs et pose le verrou si nécessaire
            Long attempts = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, Duration.ofMinutes(LOCKOUT_MINUTES));
            if (attempts != null && attempts >= MAX_FAILED_ATTEMPTS) {
                redisTemplate.opsForValue().set(lockKey, "1", Duration.ofMinutes(LOCKOUT_MINUTES));
                redisTemplate.delete(failKey);
            }
            throw e;
        }

        // Réinitialise le compteur d'échecs après un succès
        redisTemplate.delete(failKey);

        User user = userRepository.findByPhoneNumber(request.phoneNumber())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (user.isTotpEnabled()) {
            String mfaToken = jwtService.generateMfaToken(user.getPhoneNumber());
            return LoginResponse.mfaChallenge(mfaToken);
        }

        return LoginResponse.fullAuth(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                UserResponse.from(user)
        );
    }

    public AuthResponse verifyMfaLogin(MfaLoginRequest request) {
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

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
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

        return buildAuthResponse(user);
    }

    public void logout(LogoutRequest request) {
        // Révoquer le refresh token
        String refreshToken = request.refreshToken();
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Token de rafraîchissement invalide");
        }
        Date refreshExpiry = jwtService.extractExpiration(refreshToken);
        tokenBlacklistService.blacklist(refreshToken, refreshExpiry.getTime());

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

    public AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
