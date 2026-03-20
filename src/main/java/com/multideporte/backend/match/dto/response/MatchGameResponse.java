package com.multideporte.backend.match.dto.response;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;

public record MatchGameResponse(
        Long id,
        Long tournamentId,
        Long stageId,
        Long groupId,
        Integer roundNumber,
        Integer matchdayNumber,
        Long homeTournamentTeamId,
        Long awayTournamentTeamId,
        OffsetDateTime scheduledAt,
        String venueName,
        MatchGameStatus status,
        Integer homeScore,
        Integer awayScore,
        Long winnerTournamentTeamId,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
