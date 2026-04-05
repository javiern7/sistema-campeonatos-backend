package com.multideporte.backend.security.auth;

import com.multideporte.backend.security.user.AuthenticatedUser;
import java.time.OffsetDateTime;

public record AuthenticatedTokenSession(
        AuthenticatedUser authenticatedUser,
        Long sessionId,
        OffsetDateTime accessTokenExpiresAt
) {
}
