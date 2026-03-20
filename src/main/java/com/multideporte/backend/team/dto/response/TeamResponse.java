package com.multideporte.backend.team.dto.response;

import java.time.OffsetDateTime;

public record TeamResponse(
        Long id,
        String name,
        String shortName,
        String code,
        String primaryColor,
        String secondaryColor,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
