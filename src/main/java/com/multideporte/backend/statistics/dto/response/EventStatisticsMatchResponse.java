package com.multideporte.backend.statistics.dto.response;

import java.time.OffsetDateTime;

public record EventStatisticsMatchResponse(
        Long matchId,
        Long homeTournamentTeamId,
        Long awayTournamentTeamId,
        OffsetDateTime scheduledAt,
        Integer goals,
        Integer yellowCards,
        Integer redCards,
        Integer activeEvents
) {
}
