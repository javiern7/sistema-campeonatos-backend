package com.multideporte.backend.discipline.dto.response;

import java.util.List;

public record DisciplinarySanctionListResponse(
        Long tournamentId,
        int totalSanctions,
        List<DisciplinarySanctionResponse> sanctions
) {
}
