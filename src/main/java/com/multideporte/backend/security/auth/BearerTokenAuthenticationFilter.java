package com.multideporte.backend.security.auth;

import com.multideporte.backend.security.user.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final AuthTokenService authTokenService;

    public BearerTokenAuthenticationFilter(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveBearerToken(request)
                    .flatMap(authTokenService::authenticateAccessToken)
                    .ifPresent(authenticatedTokenSession -> authenticateRequest(request, authenticatedTokenSession));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequest(HttpServletRequest request, AuthenticatedTokenSession authenticatedTokenSession) {
        AuthenticatedUser user = authenticatedTokenSession.authenticatedUser();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        authentication.setDetails(new AuthSessionAuthenticationDetails(
                "BEARER",
                authenticatedTokenSession.sessionId(),
                authenticatedTokenSession.accessTokenExpiresAt()
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Optional<String> resolveBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(7).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
