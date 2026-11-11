package com.maplenou.backend.security;

import com.maplenou.backend.user.Role;
import com.maplenou.backend.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * Première connexion d'un compte du personnel : tant que le mot de passe initial n'a pas été
 * changé et que la MFA (application d'authentification) n'est pas activée, TOUT le reste de l'API
 * est refusé (403, code STAFF_ONBOARDING_REQUIRED). Seuls restent ouverts : l'authentification
 * ({@code /api/auth/**}, dont l'activation de la MFA), la lecture du profil et le changement de
 * mot de passe.
 *
 * Volontairement PAS un bean Spring : enregistré uniquement dans la chaîne de sécurité (comme
 * filtre ajouté après l'authentification JWT), sinon Spring Boot l'enregistrerait aussi comme
 * filtre servlet global, exécuté AVANT l'authentification (donc sans effet).
 *
 * La MFA du Super Admin n'est imposée ici que si {@code app.staff.enforce-mfa-super-admin=true}
 * (à activer une fois la MFA configurée sur les comptes Super Admin existants, pour ne verrouiller
 * personne). Elle est de toute façon exigée à chaque approbation de retrait.
 */
public class StaffOnboardingFilter extends OncePerRequestFilter {

    private final boolean enforceMfaForSuperAdmin;

    public StaffOnboardingFilter(boolean enforceMfaForSuperAdmin) {
        this.enforceMfaForSuperAdmin = enforceMfaForSuperAdmin;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user
                && user.getRole() != null && user.getRole().isStaff()) {

            boolean mustChangePassword = user.isMustChangePassword();
            boolean mfaSetupRequired = !user.isTotpEnabled()
                    && (user.getRole() != Role.ADMIN || enforceMfaForSuperAdmin);

            if ((mustChangePassword || mfaSetupRequired) && !isAllowedDuringOnboarding(request)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":403,"
                        + "\"error\":\"Forbidden\","
                        + "\"code\":\"STAFF_ONBOARDING_REQUIRED\","
                        + "\"message\":\"Changez votre mot de passe et activez la MFA avant d'accéder au tableau de bord.\","
                        + "\"mustChangePassword\":" + mustChangePassword + ","
                        + "\"mfaSetupRequired\":" + mfaSetupRequired + "}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDuringOnboarding(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.equals("/api/users/me") && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        return path.equals("/api/users/me/password");
    }
}
