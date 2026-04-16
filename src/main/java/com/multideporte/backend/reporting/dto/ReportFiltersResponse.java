package com.multideporte.backend.reporting.dto;

import java.time.OffsetDateTime;

public record ReportFiltersResponse(
        Long tournamentId,
        Long matchId,
        Long tournamentTeamId,
        Long teamId,
        Long playerId,
        OffsetDateTime scheduledFrom,
        OffsetDateTime scheduledTo
) {
}
