package com.multideporte.backend.publicportal.dto;

import com.multideporte.backend.match.entity.MatchGameStatus;
import java.time.OffsetDateTime;

public record PublicMatchSummaryResponse(
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
        PublicTeamSummaryResponse homeTeam,
        PublicTeamSummaryResponse awayTeam,
        PublicTeamSummaryResponse winnerTeam
) {
}
