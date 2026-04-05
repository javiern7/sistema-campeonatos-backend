package com.multideporte.backend.security.auth;

import java.time.OffsetDateTime;

public record AuthSessionAuthenticationDetails(
        String authenticationScheme,
        Long sessionId,
        OffsetDateTime accessTokenExpiresAt
) {
}
