package com.maplenou.backend.common.exception;

import io.sentry.Sentry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ResponseEntity<ErrorResponse> response = build(ex.getStatus(), ex.getMessage(), null);
        if (ex.getRetryAfterSeconds() != null) {
            return ResponseEntity.status(ex.getStatus())
                    .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                    .body(response.getBody());
        }
        return response;
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Identifiants invalides", null);
    }

    /**
     * Levée par Spring Security AVANT la vérification du mot de passe quand User.isEnabled()
     * retourne false (compte banni/anonymisé). Sans ce handler, elle tombait dans le handler
     * générique et renvoyait 500 au lieu d'un statut clair.
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Ce compte a été désactivé", null);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Ce compte est verrouillé", null);
    }

    /**
     * Un token expiré/invalide n'est jamais authentifié par JwtAuthenticationFilter (il laisse
     * passer la requête sans poser d'authentification), mais Spring Security pose alors
     * automatiquement une authentification "anonyme" par défaut. Sur une route protégée par
     * {@code @PreAuthorize("hasRole(...)")} (vendeur/livreur/admin — la grande majorité des
     * routes métier), {@code hasRole(...)} ne traite pas "anonyme" comme "non authentifié" :
     * il évalue juste que ce rôle est absent et lève AccessDeniedException → 403, alors que les
     * routes protégées au niveau HTTP (.anyRequest().authenticated()) renvoient correctement 401
     * dans le même cas. Conséquence côté client observée : en revenant sur un écran après
     * l'expiration du token (15 min), la requête échouait en 403 "Accès refusé" au lieu de 401 —
     * ApiClient/apiClient.ts ne tentent un rafraîchissement automatique que sur 401, donc
     * l'utilisateur restait bloqué jusqu'à rechargement manuel de la page/l'app. On distingue
     * donc ici ce cas (authentification absente/anonyme → 401, traité comme une session à
     * rafraîchir) d'un vrai refus de droits (utilisateur authentifié, mauvais rôle → 403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean looksUnauthenticated = authentication == null || trustResolver.isAnonymous(authentication);
        if (looksUnauthenticated) {
            return build(HttpStatus.UNAUTHORIZED, "Session expirée, veuillez vous reconnecter", null);
        }
        return build(HttpStatus.FORBIDDEN, "Accès refusé", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Données invalides", fieldErrors);
    }

    /**
     * Route inexistante (ex. /api/zzz-nope) — sans ce handler, retombait dans le handler
     * générique ci-dessous et renvoyait 500 au lieu de 404, masquant la différence entre
     * "cette route n'existe pas" et "une vraie erreur serveur" (signalé par l'audit externe
     * du 16/10/2026).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Route introuvable", null);
    }

    /** Même principe pour une méthode HTTP non supportée sur une route qui existe (ex. DELETE
     * sur un endpoint qui n'accepte que GET) : 405, pas 500. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Méthode HTTP non supportée sur cette route", null);
    }

    /** Enum invalide ou type incorrect dans un paramètre de requête/chemin (ex: ?status=HACKED
     * sur un filtre enum) — 400, pas 500 (signalé par l'audit externe du 16/10/2026). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "Valeur invalide pour le paramètre \"" + ex.getName() + "\"", null);
    }

    /** Corps de requête illisible : JSON malformé, enum invalide ou nombre hors bornes
     * (ex: quantité dépassant Integer.MAX_VALUE) dans le corps — même raison que ci-dessus. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Corps de requête invalide (champ manquant, valeur ou format incorrect)", null);
    }

    /**
     * Seul point d'entrée qui remonte encore une vraie 500 : tous les cas attendus (routes
     * inconnues, validation, auth...) ont leur propre handler ci-dessus désormais. C'est donc
     * exactement ici qu'une alerte Sentry a du sens — un bug réel, pas un 404 déguisé. No-op
     * silencieux si Sentry n'est pas configuré (voir SentryConfig).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        Sentry.captureException(ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue", null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                 List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
