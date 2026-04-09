package com.multideporte.backend.discipline.dto.response;

import com.multideporte.backend.discipline.entity.DisciplinarySanctionStatus;
import com.multideporte.backend.discipline.entity.DisciplinarySanctionType;
import java.time.OffsetDateTime;

public record DisciplinarySanctionResponse(
        Long sanctionId,
        Long incidentId,
        Long matchId,
        Long tournamentId,
        DisciplineTeamResponse team,
        DisciplinePlayerResponse player,
        DisciplinarySanctionType sanctionType,
        DisciplinarySanctionStatus status,
        Integer matchesToServe,
        Integer matchesServed,
        Integer remainingMatches,
        OffsetDateTime createdAt,
        String notes
) {
}
