package com.multideporte.backend.security.auth;

import java.util.List;

public record AuthSessionResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        List<String> roles,
        List<String> permissions
) {
}
