package com.multideporte.backend.security.basicconfiguration.dto;

import java.time.OffsetDateTime;

public record BasicConfigurationResponse(
        String organizationName,
        String supportEmail,
        String defaultTimezone,
        OffsetDateTime updatedAt
) {
}
