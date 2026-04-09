package com.multideporte.backend.discipline.dto.response;

public record DisciplinePlayerResponse(
        Long playerId,
        String fullName,
        Boolean active
) {
}
