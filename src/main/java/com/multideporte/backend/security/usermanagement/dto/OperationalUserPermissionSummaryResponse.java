package com.multideporte.backend.security.usermanagement.dto;

import java.util.List;

public record OperationalUserPermissionSummaryResponse(
        Long userId,
        String username,
        List<OperationalRoleResponse> roles,
        List<OperationalPermissionResponse> permissions
) {
}
