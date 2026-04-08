package com.multideporte.backend.security.governance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PermissionGovernanceSummaryResponse(
        OffsetDateTime generatedAt,
        boolean writeEnabled,
        List<String> mutableRoles,
        List<ManagedPermissionResponse> availablePermissions,
        List<ManagedRolePermissionResponse> roles
) {
}
