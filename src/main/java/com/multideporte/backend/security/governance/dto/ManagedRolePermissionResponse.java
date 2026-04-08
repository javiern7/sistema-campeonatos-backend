package com.multideporte.backend.security.governance.dto;

import java.util.List;

public record ManagedRolePermissionResponse(
        String roleCode,
        String roleName,
        boolean mutable,
        List<String> permissionCodes
) {
}
