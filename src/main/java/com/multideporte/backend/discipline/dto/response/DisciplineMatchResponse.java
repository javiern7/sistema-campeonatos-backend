package com.multideporte.backend.discipline.dto.response;

import java.util.List;

public record DisciplineMatchResponse(
        DisciplineMatchSummaryResponse match,
        List<DisciplinaryIncidentResponse> incidents,
        List<DisciplinarySanctionResponse> sanctions,
        DisciplineTraceabilityResponse traceability
) {
}
