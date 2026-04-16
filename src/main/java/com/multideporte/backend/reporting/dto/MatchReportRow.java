package com.multideporte.backend.reporting.dto;

import java.time.OffsetDateTime;

public record MatchReportRow(
        Long matchId,
        Long stageId,
        Long groupId,
        Integer roundNumber,
        Integer matchdayNumber,
        Long homeTournamentTeamId,
        String homeTeamName,
        Long awayTournamentTeamId,
        String awayTeamName,
        OffsetDateTime scheduledAt,
        String venueName,
        String status,
        Integer homeScore,
        Integer awayScore,
        Long winnerTournamentTeamId
) {
}
