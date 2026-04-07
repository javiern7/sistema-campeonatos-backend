package com.multideporte.backend.security.auth.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PermissionResolutionSummaryResponse(
        OffsetDateTime generatedAt,
        boolean fallbackActive,
        int rolesUsingFallbackCount,
        List<String> rolesUsingFallback,
        List<RolePermissionResolutionResponse> roles
) {
}
