package com.multideporte.backend.player.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PlayerResponse(
        Long id,
        String firstName,
        String lastName,
        String documentType,
        String documentNumber,
        LocalDate birthDate,
        String email,
        String phone,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
