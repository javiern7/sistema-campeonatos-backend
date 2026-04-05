package com.multideporte.backend.security.auth;

import java.time.OffsetDateTime;

public record AuthTokenResponse(
        String tokenType,
        String authenticationScheme,
        Long sessionId,
        String accessToken,
        OffsetDateTime accessTokenExpiresAt,
        String refreshToken,
        OffsetDateTime refreshTokenExpiresAt
) {
}
