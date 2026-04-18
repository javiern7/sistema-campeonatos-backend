package com.multideporte.backend.security.usermanagement.dto;

public record OperationalRoleResponse(
        String roleCode,
        String roleName,
        String description,
        boolean mutable,
        boolean assignable,
        String manageabilityReason
) {
}
