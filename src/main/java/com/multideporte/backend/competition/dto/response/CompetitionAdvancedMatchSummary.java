package com.multideporte.backend.competition.dto.response;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;

public record CompetitionAdvancedMatchSummary(
        Long matchId,
        Long stageId,
        String stageName,
        String stageType,
        Long groupId,
        String groupCode,
        String groupName,
        Integer roundNumber,
        Integer matchdayNumber,
        OffsetDateTime scheduledAt,
        String venueName,
        MatchGameStatus status,
        Integer homeScore,
        Integer awayScore,
        CompetitionAdvancedTeamSummary homeTeam,
        CompetitionAdvancedTeamSummary awayTeam,
        CompetitionAdvancedTeamSummary winnerTeam,
        String notes
) {
}
