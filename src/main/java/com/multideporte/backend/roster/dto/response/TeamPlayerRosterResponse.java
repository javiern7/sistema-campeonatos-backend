package com.multideporte.backend.roster.dto.response;

import com.multideporte.backend.roster.entity.RosterStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TeamPlayerRosterResponse(
        Long id,
        Long tournamentTeamId,
        Long playerId,
        Integer jerseyNumber,
        Boolean captain,
        String positionName,
        RosterStatus rosterStatus,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt
) {
}
