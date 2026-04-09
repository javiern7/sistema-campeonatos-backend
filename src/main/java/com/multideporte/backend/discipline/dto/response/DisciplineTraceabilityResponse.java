package com.multideporte.backend.discipline.dto.response;

public record DisciplineTraceabilityResponse(
        String matchDerivedFrom,
        String rosterValidationMode,
        String availabilityMode
) {
}
