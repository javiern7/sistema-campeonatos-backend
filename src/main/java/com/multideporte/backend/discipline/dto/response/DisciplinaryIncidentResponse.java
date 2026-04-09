package com.multideporte.backend.discipline.dto.response;

import com.multideporte.backend.discipline.entity.DisciplinaryIncidentType;
import java.time.OffsetDateTime;

public record DisciplinaryIncidentResponse(
        Long incidentId,
        Long matchId,
        Long tournamentId,
        DisciplineTeamResponse team,
        DisciplinePlayerResponse player,
        DisciplinaryIncidentType incidentType,
        Integer incidentMinute,
        String notes,
        OffsetDateTime createdAt,
        boolean sanctionRegistered
) {
}
