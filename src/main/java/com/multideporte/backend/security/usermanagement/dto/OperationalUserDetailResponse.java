package com.multideporte.backend.security.usermanagement.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record OperationalUserDetailResponse(
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String status,
        OffsetDateTime lastLoginAt,
        List<OperationalRoleResponse> roles,
        boolean statusManageable,
        String statusManageabilityReason,
        boolean rolesManageable,
        String rolesManageabilityReason
) {
}
