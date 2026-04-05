package com.multideporte.backend.security.auth;

import com.multideporte.backend.common.exception.BusinessException;
import com.multideporte.backend.security.user.AppRole;
import com.multideporte.backend.security.user.AppUser;
import com.multideporte.backend.security.user.AppUserRepository;
import com.multideporte.backend.security.user.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final AppUserSessionRepository appUserSessionRepository;
    private final AuthorizationCapabilityService authorizationCapabilityService;
    private final AuthSessionProperties authSessionProperties;

    @Transactional
    public AuthTokenResponse login(String username, String password, String ipAddress, String userAgent) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("Usuario autenticado no encontrado"));

        OffsetDateTime now = OffsetDateTime.now();
        user.setLastLoginAt(now);

        SessionTokens tokens = newSessionTokens();
        AppUserSession session = new AppUserSession();
        session.setUser(user);
        session.setAuthenticationScheme("BEARER");
        session.setAccessTokenHash(hashToken(tokens.accessToken()));
        session.setRefreshTokenHash(hashToken(tokens.refreshToken()));
        session.setIssuedAt(now);
        session.setLastUsedAt(now);
        session.setAccessTokenExpiresAt(now.plus(authSessionProperties.getAccessTokenTtl()));
        session.setRefreshTokenExpiresAt(now.plus(authSessionProperties.getRefreshTokenTtl()));
        session.setIpAddress(ipAddress);
        session.setUserAgent(truncate(userAgent, 255));

        AppUserSession savedSession = appUserSessionRepository.save(session);
        return toTokenResponse(savedSession, tokens);
    }

    @Transactional
    public AuthTokenResponse refresh(String refreshToken) {
        OffsetDateTime now = OffsetDateTime.now();
        AppUserSession session = appUserSessionRepository.findActiveByRefreshTokenHash(hashToken(refreshToken), now)
                .orElseThrow(() -> new BusinessException("Refresh token invalido o expirado"));

        SessionTokens rotatedTokens = newSessionTokens();
        session.setAccessTokenHash(hashToken(rotatedTokens.accessToken()));
        session.setRefreshTokenHash(hashToken(rotatedTokens.refreshToken()));
        session.setIssuedAt(now);
        session.setLastUsedAt(now);
        session.setAccessTokenExpiresAt(now.plus(authSessionProperties.getAccessTokenTtl()));
        session.setRefreshTokenExpiresAt(now.plus(authSessionProperties.getRefreshTokenTtl()));

        return toTokenResponse(session, rotatedTokens);
    }

    @Transactional
    public void logoutByAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        appUserSessionRepository.findByAccessTokenHash(hashToken(accessToken))
                .filter(session -> session.getRevokedAt() == null)
                .ifPresent(session -> session.setRevokedAt(OffsetDateTime.now()));
    }

    @Transactional
    public Optional<AuthenticatedTokenSession> authenticateAccessToken(String accessToken) {
        OffsetDateTime now = OffsetDateTime.now();
        return appUserSessionRepository.findActiveByAccessTokenHash(hashToken(accessToken), now)
                .map(session -> {
                    session.setLastUsedAt(now);
                    return new AuthenticatedTokenSession(
                            toAuthenticatedUser(session.getUser()),
                            session.getId(),
                            session.getAccessTokenExpiresAt()
                    );
                });
    }

    public AuthenticatedUser toAuthenticatedUser(AppUser user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(AppRole::getCode)
                .toList();

        Set<SimpleGrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toSet());

        authorizationCapabilityService.resolvePermissions(roleCodes)
                .stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                authorities
        );
    }

    private AuthTokenResponse toTokenResponse(AppUserSession session, SessionTokens tokens) {
        return new AuthTokenResponse(
                "Bearer",
                session.getAuthenticationScheme(),
                session.getId(),
                tokens.accessToken(),
                session.getAccessTokenExpiresAt(),
                tokens.refreshToken(),
                session.getRefreshTokenExpiresAt()
        );
    }

    private SessionTokens newSessionTokens() {
        return new SessionTokens(generateToken(), generateToken());
    }

    private String generateToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record SessionTokens(String accessToken, String refreshToken) {
    }
}
