package com.maplenou.support.identity.adapter.in.security;

import com.maplenou.support.identity.application.UserDirectory;
import com.maplenou.support.platform.security.AuthenticatedRequestFilter;
import com.maplenou.support.platform.security.CurrentActorProvider;
import com.maplenou.support.platform.web.ApiExceptionHandler;
import com.maplenou.support.shared.SupportException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * After token validation: refreshes the caller's local projection and rejects users deactivated by an
 * administrator. Contributed to the security chain (platform {@code SecurityConfig}).
 */
public class UserSyncFilter extends OncePerRequestFilter implements AuthenticatedRequestFilter {

    private final UserDirectory directory;

    public UserSyncFilter(UserDirectory directory) {
        this.directory = directory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            boolean active;
            try {
                active = directory.sync(CurrentActorProvider.fromJwt(jwt));
            } catch (SupportException e) {
                write(response, ApiExceptionHandler.statusOf(e.kind()).value(), e.code(), e.getMessage());
                return;
            }
            if (!active) {
                write(response, 403, "ACCOUNT_DISABLED", "Ce compte support est désactivé.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void write(HttpServletResponse response, int status, String code, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\",\"detail\":\""
                + detail.replace("\"", "'") + "\"}");
    }
}
