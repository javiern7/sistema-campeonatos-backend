package com.multideporte.backend.security.governance.dto;

public record ManagedPermissionResponse(
        String code,
        String name,
        String description
) {
}
