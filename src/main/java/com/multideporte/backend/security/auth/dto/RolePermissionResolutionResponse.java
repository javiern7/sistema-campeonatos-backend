package com.multideporte.backend.security.auth.dto;

import java.util.List;

public record RolePermissionResolutionResponse(
        String roleCode,
        int persistedPermissionCount,
        int effectivePermissionCount,
        boolean usingFallback,
        List<String> persistedPermissions,
        List<String> effectivePermissions
) {
}
