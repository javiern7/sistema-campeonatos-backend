package com.multideporte.backend.security.auth;

import java.util.List;

public record AuthSessionResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String authenticationScheme,
        String sessionStrategy,
        Long sessionId,
        java.time.OffsetDateTime accessTokenExpiresAt,
        List<String> roles,
        List<String> permissions
) {
}
