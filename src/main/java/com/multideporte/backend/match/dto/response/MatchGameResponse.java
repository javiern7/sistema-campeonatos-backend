package com.multideporte.backend.match.dto.response;

import com.multideporte.backend.match.entity.MatchGameStatus;
import com.multideporte.backend.match.entity.MatchPurpose;
import com.multideporte.backend.match.entity.MatchResolutionMethod;
import com.multideporte.backend.match.entity.MatchSourceOutcome;
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
        Integer homePenaltyScore,
        Integer awayPenaltyScore,
        MatchResolutionMethod resolutionMethod,
        MatchPurpose matchPurpose,
        Integer bracketPosition,
        Long homeSourceMatchId,
        MatchSourceOutcome homeSourceOutcome,
        Long awaySourceMatchId,
        MatchSourceOutcome awaySourceOutcome,
        Long winnerTournamentTeamId,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
