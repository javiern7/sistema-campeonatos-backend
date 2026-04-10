package com.multideporte.backend.security.usermanagement.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OperationalUserResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String status,
        OffsetDateTime lastLoginAt,
        List<String> roles,
        boolean statusManageable
) {
}
